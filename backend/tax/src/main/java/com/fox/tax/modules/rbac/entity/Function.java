package com.fox.tax.modules.rbac.entity;

import com.fox.tax.common.entity.AbstractPersistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "sys_function")
@EqualsAndHashCode(callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Function extends AbstractPersistable {

    @Column(unique = true, nullable = false)
    private String code; // e.g., USER_VIEW, USER_EDIT

    private String name;

    private String description;
}
