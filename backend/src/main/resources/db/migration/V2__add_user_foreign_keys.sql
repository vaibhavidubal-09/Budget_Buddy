ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_user
    FOREIGN KEY (user_id) REFERENCES app_users (id)
    ON DELETE RESTRICT;

ALTER TABLE income_entries
    ADD CONSTRAINT fk_income_entries_user
    FOREIGN KEY (user_id) REFERENCES app_users (id)
    ON DELETE RESTRICT;

ALTER TABLE category_budgets
    ADD CONSTRAINT fk_category_budgets_user
    FOREIGN KEY (user_id) REFERENCES app_users (id)
    ON DELETE RESTRICT;
