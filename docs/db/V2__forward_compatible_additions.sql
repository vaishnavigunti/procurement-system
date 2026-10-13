-- =====================================================================
-- V2: Additive changes (run AFTER schema.sql and seed.sql)
-- Purpose: add the columns and sequences that later milestones need, now,
-- so we never have to edit finished code or tables again.
-- Safe to run more than once (IF NOT EXISTS).
-- Run as procurement_user against procurement_db.
-- =====================================================================

-- Supplier status changes need a reason (PATCH /suppliers/{id}/status)   [Milestone 2]
ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);

-- Requisition cancel needs a reason (POST /requisitions/{id}/cancel)     [Milestone 2]
ALTER TABLE purchase_requisitions
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);

-- Purchase order lifecycle details                                      [Milestone 3]
-- and ERP references for integration                                    [Milestone 4]
ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS confirmed_delivery_date DATE,
    ADD COLUMN IF NOT EXISTS cancel_reason           VARCHAR(500),
    ADD COLUMN IF NOT EXISTS closure_comments        VARCHAR(500),
    ADD COLUMN IF NOT EXISTS external_ref            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS external_payment_status VARCHAR(30);

-- Business numbers (SUP-00001, PR-2026-00001, PO-2026-00001) come from
-- database sequences, which are safe when several users create records at once.
CREATE SEQUENCE IF NOT EXISTS supplier_code_seq         START 1;
CREATE SEQUENCE IF NOT EXISTS requisition_number_seq    START 1;
CREATE SEQUENCE IF NOT EXISTS purchase_order_number_seq START 1;
