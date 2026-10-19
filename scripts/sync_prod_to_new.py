#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
正式機 (fox_tax) 至 新版退稅系統 (fox_refund) 資料同步與重置腳本

用途：
1. 於雙軌並行測試期間，定期（如每週）將舊版正式機最新資料同步至新版資料庫。
2. 自動清除新版資料庫在測試期間產生的業務操作資料（僅重置 4 張退稅業務表，保留 sys_user 等系統權限表）。
3. 自動對齊欄位精度（將 import_declaration.total_refund_qty 升級為 DECIMAL(19,3)）。
4. 支援 --dry-run 預覽模式與 --yes 免確認模式。
"""

import argparse
import os
import sys
import time
import pymysql
import pymysql.cursors

# 確保 Windows 終端機 (PowerShell / CMD) 輸出 UTF-8 中文不亂碼
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")


def load_dotenv_file():
    """自動從腳本同目錄或專案根目錄讀取 .env 環境變數檔（不覆寫已存在的系統環境變數）"""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        os.path.join(script_dir, ".env"),
        os.path.join(os.path.dirname(script_dir), ".env"),
    ]
    for env_path in candidates:
        if os.path.isfile(env_path):
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith("#") or "=" not in line:
                        continue
                    k, v = line.split("=", 1)
                    os.environ.setdefault(k.strip(), v.strip().strip("'\""))


load_dotenv_file()

# 來源：舊版正式機資料庫設定（唯讀查詢，絕不寫入；密碼由 .env 或環境變數 PROD_DB_PASS 注入）
PROD_CONFIG = {
    "host": os.environ.get("PROD_DB_HOST", "35.194.138.29"),
    "port": int(os.environ.get("PROD_DB_PORT", "3306")),
    "user": os.environ.get("PROD_DB_USER", "fox"),
    "password": os.environ.get("PROD_DB_PASS", ""),
    "database": os.environ.get("PROD_DB_NAME", "fox_tax"),
    "charset": "utf8mb4",
    "connect_timeout": 15,
    "read_timeout": 300,
}

# 目標：新版退稅系統資料庫設定（清空業務表並寫入最新快照）
NEW_CONFIG = {
    "host": os.environ.get("NEW_DB_HOST", "192.168.2.79"),
    "port": int(os.environ.get("NEW_DB_PORT", "33061")),
    "user": os.environ.get("NEW_DB_USER", "fox"),
    "password": os.environ.get("NEW_DB_PASS", "Foxgogo2018"),
    "database": os.environ.get("NEW_DB_NAME", "fox_refund"),
    "charset": "utf8mb4",
    "connect_timeout": 15,
    "write_timeout": 300,
}

# 定義要同步的 4 張核心業務表與欄位映射設定
SYNC_TABLES = [
    {
        "prod_table": "fox_tax_bom",
        "new_table": "tax_bom",
        "desc": "核退標準 (BOM)",
        "select_sql": """
            SELECT
                ID, CREATE_TIME, CREATOR, UPDATE_TIME, UPDATOR,
                doc_no, prod_type, prod_name, prod_unit,
                material_num, material_name, material_unit, material_spec, usage_qty
            FROM fox_tax_bom
            ORDER BY ID ASC
        """,
        "insert_sql": """
            INSERT INTO tax_bom (
                id, create_time, creator, update_time, updator,
                doc_no, prod_type, prod_name, prod_unit,
                material_num, material_name, material_unit, material_spec, usage_qty
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
    },
    {
        "prod_table": "fox_import_declaration",
        "new_table": "import_declaration",
        "desc": "進口報單",
        "select_sql": """
            SELECT
                ID, CREATE_TIME, CREATOR, UPDATE_TIME, UPDATOR,
                doc_no, items, material_name, material_unit, material_spec,
                import_qty, COALESCE(total_refund_qty, 0.000)
            FROM fox_import_declaration
            ORDER BY ID ASC
        """,
        "insert_sql": """
            INSERT INTO import_declaration (
                id, create_time, creator, update_time, updator,
                doc_no, items, material_name, material_unit, material_spec,
                import_qty, total_refund_qty
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
    },
    {
        "prod_table": "fox_export_declaration",
        "new_table": "export_declaration",
        "desc": "出口報單",
        "select_sql": """
            SELECT
                ID, CREATE_TIME, CREATOR, UPDATE_TIME, UPDATOR,
                doc_no, items, prod_type, prod_name, export_qty, status
            FROM fox_export_declaration
            ORDER BY ID ASC
        """,
        "insert_sql": """
            INSERT INTO export_declaration (
                id, create_time, creator, update_time, updator,
                doc_no, items, prod_type, prod_name, export_qty, status
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
    },
    {
        "prod_table": "fox_tax_refund",
        "new_table": "tax_refund",
        "desc": "退稅核銷紀錄",
        "select_sql": """
            SELECT
                ID, CREATE_TIME, CREATOR, UPDATE_TIME, UPDATOR,
                report_no, doc_no, export_declaration_id, import_declaration_id,
                usage_qty, branch_num, tax_bom_id
            FROM fox_tax_refund
            ORDER BY ID ASC
        """,
        "insert_sql": """
            INSERT INTO tax_refund (
                id, create_time, creator, update_time, updator,
                report_no, doc_no, export_declaration_id, import_declaration_id,
                usage_qty, branch_num, tax_bom_id
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
    },
]


def get_table_counts(conn, table_names):
    """取得指定資料表的目前資料筆數"""
    counts = {}
    with conn.cursor() as cur:
        for t in table_names:
            cur.execute(f"SELECT COUNT(*) FROM `{t}`")
            counts[t] = cur.fetchone()[0]
    return counts


def ensure_schema_precision(new_conn):
    """確保新版資料庫欄位精度對齊正式機 DECIMAL(19,3) 並補齊時間查詢索引"""
    with new_conn.cursor() as cur:
        cur.execute(
            """
            ALTER TABLE `import_declaration`
            MODIFY COLUMN `total_refund_qty` DECIMAL(19,3) NOT NULL DEFAULT 0.000
            """
        )
        indexes_to_ensure = [
            ("import_declaration", "idx_import_dec_create_time", "create_time"),
            ("export_declaration", "idx_export_dec_create_time", "create_time"),
            ("export_declaration", "idx_export_dec_update_time", "update_time"),
            ("tax_refund", "idx_tax_refund_create_time", "create_time"),
        ]
        for table_name, idx_name, col_name in indexes_to_ensure:
            cur.execute(f"SHOW INDEX FROM `{table_name}` WHERE Key_name = %s", (idx_name,))
            if not cur.fetchall():
                cur.execute(f"CREATE INDEX `{idx_name}` ON `{table_name}` (`{col_name}`)")
    new_conn.commit()


def sync_data(batch_size=5000, dry_run=False, auto_yes=False):
    """執行正式機至新版資料庫的全量同步與測試資料重置"""
    print("=" * 72)
    print("  進口退稅系統 - 正式機 (fox_tax) -> 新版系統 (fox_refund) 資料同步工具")
    print("=" * 72)
    print(f"  [來源-唯讀] {PROD_CONFIG['host']}:{PROD_CONFIG['port']} / {PROD_CONFIG['database']}")
    print(f"  [目標-寫入] {NEW_CONFIG['host']}:{NEW_CONFIG['port']} / {NEW_CONFIG['database']}")
    print("-" * 72)

    # 安全檢查：防止誤將目標設為正式機 IP
    if NEW_CONFIG["host"] == "35.194.138.29" or NEW_CONFIG["database"] == "fox_tax":
        print("[錯誤] 目標資料庫設定指向正式機，已觸發安全鎖定並中止執行！")
        sys.exit(1)

    prod_conn = pymysql.connect(**PROD_CONFIG)
    new_conn = pymysql.connect(**NEW_CONFIG)

    try:
        # 強制將來源連線設為唯讀交易模式，雙重保障正式機安全
        with prod_conn.cursor() as p_cur:
            p_cur.execute("SET SESSION TRANSACTION READ ONLY")

        prod_counts = get_table_counts(prod_conn, [t["prod_table"] for t in SYNC_TABLES])
        new_counts = get_table_counts(new_conn, [t["new_table"] for t in SYNC_TABLES])

        print("\n【同步前資料筆數比對】")
        print(f"  {'業務表名稱':<18} | {'正式機 (來源)':>15} | {'新版系統 (目前)':>15}")
        print("  " + "-" * 58)
        for item in SYNC_TABLES:
            p_cnt = prod_counts[item["prod_table"]]
            n_cnt = new_counts[item["new_table"]]
            print(f"  {item['new_table']:<20} | {p_cnt:>15,d} | {n_cnt:>15,d}")
        print("  " + "-" * 58)
        print("  * 註：新版系統的帳號權限表 (sys_user, sys_role 等) 將完整保留，不受影響。\n")

        if dry_run:
            print("[預覽模式 --dry-run] 僅檢視筆數差異，未對資料庫進行任何變更。")
            return

        if not auto_yes:
            ans = input("確定要清除新版系統上述 4 張業務表的測試資料，並從正式機完整同步嗎？ (y/N): ").strip().lower()
            if ans not in ("y", "yes"):
                print("已取消同步作業。")
                return

        start_total = time.time()

        print("\n[步驟 1/3] 檢查並對齊新版資料庫欄位精度 (DECIMAL(19,3))...")
        ensure_schema_precision(new_conn)

        print("[步驟 2/3] 清空新版系統 4 張退稅業務表之測試操作紀錄...")
        with new_conn.cursor() as n_cur:
            n_cur.execute("SET FOREIGN_KEY_CHECKS = 0")
            n_cur.execute("SET UNIQUE_CHECKS = 0")
            # 依外鍵相依順序反序清空
            for item in reversed(SYNC_TABLES):
                t_name = item["new_table"]
                print(f"  -> TRUNCATE TABLE `{t_name}` ...")
                n_cur.execute(f"TRUNCATE TABLE `{t_name}`")
            new_conn.commit()

        print(f"[步驟 3/3] 開始批次同步正式機資料 (每批次 {batch_size:,d} 筆)...")
        for item in SYNC_TABLES:
            p_table = item["prod_table"]
            n_table = item["new_table"]
            total_expected = prod_counts[p_table]
            t0 = time.time()
            copied = 0

            # 使用 SSCursor 串流讀取來源資料，避免大表佔用過多記憶體
            with prod_conn.cursor(pymysql.cursors.SSCursor) as s_cur, new_conn.cursor() as n_cur:
                n_cur.execute("SET FOREIGN_KEY_CHECKS = 0")
                n_cur.execute("SET UNIQUE_CHECKS = 0")
                s_cur.execute(item["select_sql"])

                while True:
                    rows = s_cur.fetchmany(batch_size)
                    if not rows:
                        break
                    n_cur.executemany(item["insert_sql"], rows)
                    new_conn.commit()
                    copied += len(rows)
                    pct = (copied / total_expected * 100) if total_expected > 0 else 100.0
                    elapsed = time.time() - t0
                    print(
                        f"\r  -> 同步 `{n_table}` ({item['desc']}): "
                        f"{copied:,d} / {total_expected:,d} ({pct:5.1f}%) - 耗時 {elapsed:.1f}s",
                        end="",
                        flush=True,
                    )
                print()

        # 恢復外鍵與唯一性檢查
        with new_conn.cursor() as n_cur:
            n_cur.execute("SET UNIQUE_CHECKS = 1")
            n_cur.execute("SET FOREIGN_KEY_CHECKS = 1")
        new_conn.commit()

        # 驗證同步後筆數
        after_counts = get_table_counts(new_conn, [t["new_table"] for t in SYNC_TABLES])
        total_elapsed = time.time() - start_total

        print("\n" + "=" * 72)
        print(f"  同步完成！總耗時：{total_elapsed:.1f} 秒")
        print("=" * 72)
        for item in SYNC_TABLES:
            p_cnt = prod_counts[item["prod_table"]]
            a_cnt = after_counts[item["new_table"]]
            status = "OK" if p_cnt == a_cnt else "MISMATCH"
            print(f"  - {item['new_table']:<22}: {a_cnt:>10,d} 筆 (正式機: {p_cnt:>10,d} 筆) [{status}]")
        print("=" * 72)

    finally:
        prod_conn.close()
        new_conn.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="正式機 (fox_tax) 至 新版退稅系統 (fox_refund) 資料同步工具")
    parser.add_argument("--batch-size", type=int, default=5000, help="每批次寫入筆數 (預設: 5000)")
    parser.add_argument("--dry-run", action="store_true", help="僅預覽雙方資料筆數，不執行清空與寫入")
    parser.add_argument("-y", "--yes", action="store_true", help="跳過互動確認直接執行同步")
    args = parser.parse_args()

    sync_data(batch_size=args.batch_size, dry_run=args.dry_run, auto_yes=args.yes)
