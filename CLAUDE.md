# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

進口退稅核銷管理系統 (Tax Refund Management System) — helps OP personnel manage import/export declarations, maintain BOM tables, and automatically calculate tax refund settlement amounts.

## Tech Stack

- **Backend:** Java 17+, Spring Boot 3.2, Spring Data JPA, QueryDSL 5, Spring Security + JWT, Maven, Lombok, MapStruct, Apache POI
- **Frontend:** Vue 3 (Composition API, `<script setup>`), Vite, Element Plus, Pinia, Axios, Vue Router
- **Database:** MySQL 8.0 (schema: `fox_refund`)

## Commands

### Backend (`backend/tax/`)
```bash
cd backend/tax
./mvnw spring-boot:run          # Run dev server (port 8080)
./mvnw clean package            # Build JAR
./mvnw test                     # Run all tests
./mvnw test -Dtest=ClassName    # Run single test class
```

### Frontend (`frontend/`)
```bash
cd frontend
npm install                     # Install dependencies
npm run dev                     # Dev server (port 5173, proxies /api to :8080)
npm run build                   # Production build
npm run preview                 # Preview production build
```

## Architecture

Package-by-feature layout with layered architecture within each module:

```
backend/.../com/fox/tax/
├── common/                     # Cross-cutting: SecurityConfig, JWT, GlobalExceptionHandler, AbstractPersistable
└── modules/
    ├── rbac/                   # User/Role/Function management + authentication
    └── refund/                 # Core business: TaxBom, ImportDeclaration, ExportDeclaration, TaxRefund

frontend/src/
├── api/                        # All HTTP calls (auth, refund/*, system/*)
├── layout/                     # Navbar, Sidebar, AppMain, TagsView
├── router/                     # Route definitions + permission guards
├── stores/                     # Pinia: user, permission, tagsView
├── directive/permission/       # v-permission directive for UI element visibility
├── utils/                      # request.js (Axios interceptors), auth.js (token helpers)
└── views/                      # Pages: login, dashboard, system/*, refund/*
```

### Data flow
Frontend (Axios + JWT header) → Spring Security filter (validates JWT) → Controller (input validation) → Service (business logic, MapStruct mapping) → Repository (JPA/QueryDSL) → MySQL

### Authentication
JWT-based stateless auth. Token stored in localStorage via `getToken()/setToken()`. Axios interceptor adds `Authorization: Bearer <token>`. Backend filter validates on every request. Permissions enforced via `@PreAuthorize` (backend) and `v-permission` directive + route guards (frontend).

### Entity-DTO pattern
All entities extend `AbstractPersistable` (id, creator, updator, createTime, updateTime). Services must return DTOs (via MapStruct mappers), never entities. Controller layer only validates input format; business logic belongs in services.

## Code Style

- **Backend:** Google Java Format (https://google.github.io/styleguide/javaguide.html)
- **Frontend:** Vue.js Style Guide (https://vuejs.org/style-guide/)
- **UI text:** All labels, placeholders, and messages must be in Traditional Chinese (繁體中文)
- **UI components:** Use Element Plus exclusively

## Development Conventions

- New features: define Entity + Repository first, then Service, then Controller + DTO
- Excel import: use Apache POI with merged-cell handling (see `TaxBomService` for reference)
- API calls in frontend must be in `src/api/`, never direct axios in components
- Passwords must use BCrypt encryption
- API endpoints require `@PreAuthorize` for access control
- Use `@Transactional` on service methods that modify data
