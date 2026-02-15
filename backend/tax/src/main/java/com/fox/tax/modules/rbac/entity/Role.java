package com.fox.tax.modules.rbac.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.Set;

import com.fox.tax.common.entity.AbstractPersistable;

@Entity
@Data
@Table(name = "sys_role")
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Role extends AbstractPersistable {

    @Column(unique = true, nullable = false)
    private String name; // 例如: ROLE_OP, ROLE_ADMIN

    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sys_role_functions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "function_id"))
    private Set<Function> functions;
}