CREATE UNIQUE INDEX uk_category_budgets_user_period_category
    ON category_budgets (user_id, period, category);
