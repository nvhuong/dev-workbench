package com.nvhuong.devworkbench.util;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SqlFormatterTest {

    @Test(description = "Các phương ngữ phổ biến nên được định dạng đơn giản SELECT")
    public void shouldFormatBasicSelectExactly() {
        assertFormattedEquals("""
                        SELECT id,
                          name
                          FROM users
                            WHERE status = 1
                              AND age > 18
                              ORDER BY created_at DESC;""",
                "select id,name from users where status=1 and age>18 order by created_at desc");
    }

    @Test(description = "Phương ngữ phổ biến nên được định dạng JOIN Truy vấn")
    public void shouldFormatJoinQueryExactly() {
        assertFormattedEquals("""
                        SELECT u.id,
                          u.name,
                          o.total
                          FROM users u
                            LEFT JOIN orders o ON u.id = o.user_id
                            WHERE o.total > 100
                              ORDER BY o.total DESC;""",
                "select u.id,u.name,o.total from users u left join orders o on u.id=o.user_id where o.total>100 order by o.total desc");
    }

    @Test(description = "Các phương ngữ phổ biến phải được định dạng phức tạp bằng cách thụt dòng theo tầng SELECT")
    public void shouldFormatComplexSelectWithCascadeIndentExactly() {
        assertFormattedEquals("""
                        SELECT u.id,
                          u.name,
                          u.email,
                          u.created_at,
                          o.order_id,
                          o.total,
                          o.status
                          FROM users u
                            LEFT JOIN orders o ON u.id = o.user_id
                            WHERE u.status = 1
                              AND u.created_at >= '2024-01-01'
                              AND(o.total > 100 OR o.status = 'paid')
                              GROUP BY u.id
                              HAVING count(o.order_id) > 0
                              ORDER BY u.created_at DESC,
                              o.total DESC
                            LIMIT 100;""",
                "select u.id,u.name,u.email,u.created_at,o.order_id,o.total,o.status from users u left join orders o on u.id=o.user_id where u.status=1 and u.created_at>='2024-01-01' and (o.total>100 or o.status='paid') group by u.id having count(o.order_id)>0 order by u.created_at desc,o.total desc limit 100");
    }

    @Test(description = "Định dạng nhiều câu lệnh nên được hỗ trợ")
    public void shouldFormatMultipleStatementsExactly() {
        assertFormattedEquals("""
                        SELECT 1;

                        UPDATE users
                        SET name = 'alice'
                        WHERE id = 1;""",
                "select 1; update users set name='alice' where id=1");
    }

    @Test(description = "Phương ngữ phổ quát phải được định dạng với các truy vấn con phức tạp SELECT")
    public void shouldFormatNestedSelectExactly() {
        assertFormattedEquals("""
                        SELECT u.id,
                          (
                            SELECT count(*)
                            FROM orders o
                            WHERE o.user_id = u.id
                          ) AS order_count
                          FROM users u
                            WHERE u.status = 'ACTIVE';""",
                "select u.id,(select count(*) from orders o where o.user_id=u.id) as order_count from users u where u.status='ACTIVE'");
    }

    @Test(description = "Phương ngữ phổ biến nên được định dạng CTE và CASE biểu hiện")
    public void shouldFormatCteAndCaseExactly() {
        assertFormattedEquals("""
                        WITH active_users AS (
                          SELECT id,
                            name,
                            status
                          FROM users
                          WHERE status = 'ACTIVE'
                        )
                        SELECT id,
                          CASE
                            WHEN status = 'ACTIVE' THEN 'Y'
                            ELSE 'N'
                          END AS enabled_flag
                          FROM active_users;""",
                "with active_users as (select id,name,status from users where status='ACTIVE') select id,case when status='ACTIVE' then 'Y' else 'N' end as enabled_flag from active_users");
    }

    @Test(description = "Các tùy chọn tùy chỉnh sẽ hỗ trợ các từ khóa viết thường và loại bỏ dấu chấm phẩy ở cuối")
    public void shouldRespectLowercaseAndNoSemicolonOption() {
        String formatted = SqlFormatter.format("SELECT id,name FROM users WHERE status=1",
                new SqlFormatter.FormatOption()
                        .setUppercaseKeywords(false)
                        .setAddSemicolon(false));

        assertEquals(formatted, """
                select id,
                  name
                  from users
                    where status = 1""");
    }

    @Test(description = "Việc thu nhỏ sẽ loại bỏ nhận xét và nén thành một dòng")
    public void shouldCompressSqlWithoutComments() {
        String input = """
                SELECT id, -- user id
                  name /* user name */
                FROM users
                WHERE status = 1;
                """;

        String compressed = SqlFormatter.compress(input);

        assertEquals(compressed, "SELECT id, name FROM users WHERE status = 1;");
        assertFalse(compressed.contains("\n"));
    }

    @Test(description = "Nén nên bảo toàn dấu chấm phẩy nhiều câu lệnh")
    public void shouldCompressMultipleStatements() {
        String compressed = SqlFormatter.compress("select 1;\nupdate users set name='a' where id=1;");
        assertEquals(compressed, "select 1; update users set name = 'a' where id = 1;");
    }

    @Test(description = "Việc xác thực sẽ xác định các biểu thức bị thiếu WHERE khoản")
    public void shouldDetectIncompleteWhereClause() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("select * from users where");

        assertFalse(result.valid());
        assertTrue(result.issues().contains(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "WHERE")));
    }

    @Test(description = "Việc xác minh sẽ xác định được sự thiếu sót SET của UPDATE")
    public void shouldDetectUpdateWithoutSet() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("update users where id=1");

        assertFalse(result.valid());
        assertTrue(result.issues().contains(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_MISSING_KEYWORD, "SET")));
    }

    @Test(description = "Việc xác minh sẽ xác định được sự thiếu sót FROM của DELETE")
    public void shouldDetectDeleteWithoutFrom() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("delete users where id=1");

        assertFalse(result.valid());
        assertTrue(result.issues().contains(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_KEYWORD_AFTER, "FROM", "DELETE")));
    }

    @Test(description = "Việc xác minh phải được công nhận GROUP mất tích sau đó BY")
    public void shouldDetectMissingByAfterGroup() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("select id from users group id");

        assertFalse(result.valid());
        assertTrue(result.issues().contains(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_KEYWORD_AFTER, "BY", "GROUP")));
    }

    @Test(description = "Việc xác minh phải được công nhận SELECT Biểu thức bị thiếu sau")
    public void shouldDetectSelectWithoutProjection() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("select from users");

        assertFalse(result.valid());
        assertTrue(result.issues().contains(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "SELECT")));
    }

    @Test(description = "Việc xác minh phải được công nhận INSERT mất tích sau đó VALUES hoặc SELECT")
    public void shouldDetectInsertWithoutValuesOrSelect() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("insert into users(id,name)");

        assertFalse(result.valid());
        assertTrue(result.issues().contains(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_MISSING_KEYWORD, "VALUES/SELECT")));
    }

    @Test(description = "Việc xác thực sẽ xác định các chuỗi không được tiết lộ")
    public void shouldDetectUnterminatedString() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("select * from users where name='alice");

        assertFalse(result.valid());
        assertEquals(result.firstIssue(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_UNTERMINATED_STRING));
    }

    @Test(description = "Việc xác thực sẽ xác định các nhận xét khối không được tiết lộ")
    public void shouldDetectUnterminatedComment() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate("select /* test");

        assertFalse(result.valid());
        assertEquals(result.firstIssue(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_UNTERMINATED_COMMENT));
    }

    @Test(description = "Việc xác minh nên đếm số lượng nhiều câu lệnh")
    public void shouldCountStatementsDuringValidation() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate(
                "select 1; update users set name='alice' where id=1;");

        assertTrue(result.valid(), result.firstIssue());
        assertEquals(result.statementCount(), 2);
    }

    @Test(description = "Việc xác minh phải được cho phép INSERT SELECT tuyên bố")
    public void shouldValidateInsertSelect() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate(
                "insert into archive_users(id,name) select id,name from users where status=1");

        assertTrue(result.valid(), result.firstIssue());
    }

    @Test(description = "MySQL Các phương ngữ nên hỗ trợ các bình luận đánh dấu ngược và đánh dấu")
    public void shouldFormatMysqlDialectSql() {
        String formatted = SqlFormatter.format("select `id`,`name` # comment\nfrom `users` limit 10",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                SELECT `id`,
                  `name`
                  # comment
                  FROM `users`
                  LIMIT 10;""");
    }

    @Test(description = "MySQL Các phương ngữ nên được định dạng ON DUPLICATE KEY UPDATE")
    public void shouldFormatMysqlOnDuplicateKeyUpdate() {
        String formatted = SqlFormatter.format(
                "insert into users(id,name,status) values(1,'alice',1) on duplicate key update name='alice2',status=2",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                INSERT INTO users (id, name, status)
                VALUES (1, 'alice', 1)
                ON DUPLICATE KEY UPDATE name = 'alice2',
                  status = 2;""");
    }

    @Test(description = "MySQL Các phương ngữ nên được định dạng INSERT IGNORE")
    public void shouldFormatMysqlInsertIgnore() {
        String formatted = SqlFormatter.format(
                "insert ignore into users(id,name,status) values(1,'alice',1)",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                INSERT IGNORE INTO users (id, name, status)
                VALUES (1, 'alice', 1);""");
    }

    @Test(description = "MySQL Các phương ngữ nên được định dạng REPLACE INTO")
    public void shouldFormatMysqlReplaceInto() {
        String formatted = SqlFormatter.format(
                "replace into users(id,name,status) values(1,'alice',1)",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                REPLACE INTO users (id, name, status)
                VALUES (1, 'alice', 1);""");
    }

    @Test(description = "MySQL Các phương ngữ nên được định dạng STRAIGHT_JOIN")
    public void shouldFormatMysqlStraightJoin() {
        String formatted = SqlFormatter.format(
                "select u.id,u.name from users u straight_join orders o on u.id=o.user_id where o.status='paid'",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                SELECT u.id,
                  u.name
                  FROM users u
                    STRAIGHT_JOIN orders o ON u.id = o.user_id
                    WHERE o.status = 'paid';""");
    }

    @Test(description = "MySQL Các phương ngữ nên được định dạng USE INDEX và FOR UPDATE")
    public void shouldFormatMysqlUseIndexAndForUpdate() {
        String formatted = SqlFormatter.format(
                "select u.id,u.name from users u use index(idx_users_status) where u.status=1 for update",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                SELECT u.id,
                  u.name
                  FROM users u USE INDEX (idx_users_status)
                    WHERE u.status = 1
                    FOR UPDATE;""");
    }

    @Test(description = "MySQL Các phương ngữ nên được định dạng FORCE INDEX và LOCK IN SHARE MODE")
    public void shouldFormatMysqlForceIndexAndLockShareMode() {
        String formatted = SqlFormatter.format(
                "select u.id,u.name from users u force index(idx_users_status) where u.status=1 lock in share mode",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                SELECT u.id,
                  u.name
                  FROM users u FORCE INDEX (idx_users_status)
                    WHERE u.status = 1
                    LOCK IN SHARE MODE;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ phải hỗ trợ các mã định danh được trích dẫn kép và  RETURNING")
    public void shouldFormatPostgresqlDialectSql() {
        String formatted = SqlFormatter.format("select \"id\",\"name\" from \"users\" returning \"id\"",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT "id",
                  "name"
                  FROM "users"
                  RETURNING "id";""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng WITH RECURSIVE và ILIKE")
    public void shouldFormatPostgresqlRecursiveCteAndIlike() {
        String formatted = SqlFormatter.format(
                "with recursive dept_tree as (select id,parent_id,name from departments where parent_id is null union all select d.id,d.parent_id,d.name from departments d join dept_tree t on d.parent_id=t.id) select * from dept_tree where name ilike 'ops%'",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                WITH RECURSIVE dept_tree AS (
                  SELECT id,
                    parent_id,
                    name
                  FROM departments
                  WHERE parent_id IS NULL
                  UNION ALL SELECT d.id,
                    d.parent_id,
                    d.name
                  FROM departments d
                    JOIN dept_tree t ON d.parent_id = t.id
                )
                SELECT *
                  FROM dept_tree
                    WHERE name ILIKE 'ops%';""");
    }

    @Test(description = "PostgreSQL Các phương ngữ cần được bảo tồn :: cast ngữ pháp")
    public void shouldFormatPostgresqlTypeCast() {
        String formatted = SqlFormatter.format(
                "select id::bigint,name::text from users where created_at::date >= '2024-01-01'::date",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT id::bigint,
                  name::text
                  FROM users
                    WHERE created_at::date >= '2024-01-01'::date;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng DISTINCT ON và NULLS LAST")
    public void shouldFormatPostgresqlDistinctOnAndNullsLast() {
        String formatted = SqlFormatter.format(
                "select distinct on (u.status) u.id,u.name,u.status from users u order by u.status,u.created_at desc nulls last",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT DISTINCT ON (u.status) u.id,
                  u.name,
                  u.status
                  FROM users u
                    ORDER BY u.status,
                    u.created_at DESC NULLS LAST;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng FILTER khoản")
    public void shouldFormatPostgresqlFilterClause() {
        String formatted = SqlFormatter.format(
                "select count(*) filter (where status='paid') as paid_count,sum(total) filter (where total>100) as high_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT count(*) FILTER (
                    WHERE status = 'paid'
                  ) AS paid_count,
                  sum(total) FILTER (
                    WHERE total > 100
                  ) AS high_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên định dạng mệnh đề chức năng cửa sổ")
    public void shouldFormatPostgresqlWindowClause() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at rows between unbounded preceding and current row) as running_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                  ) AS running_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng RANGE cửa sổ frame")
    public void shouldFormatPostgresqlWindowRangeClause() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at range between unbounded preceding and current row) as running_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at
                    RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                  ) AS running_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng GROUPS cửa sổ frame")
    public void shouldFormatPostgresqlWindowGroupsClause() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at groups between unbounded preceding and current row) as running_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at
                    GROUPS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                  ) AS running_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng FILTER và OVER sự kết hợp")
    public void shouldFormatPostgresqlFilterOverWindowClause() {
        String formatted = SqlFormatter.format(
                "select count(*) filter (where status='paid') over (partition by team_id order by created_at rows between unbounded preceding and current row) as paid_running_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT count(*) FILTER (
                    WHERE status = 'paid'
                  ) OVER (
                    PARTITION BY team_id
                    ORDER BY created_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                  ) AS paid_running_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng FOLLOWING cửa sổ frame")
    public void shouldFormatPostgresqlWindowFollowingClause() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at rows between current row and unbounded following) as forward_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at
                    ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
                  ) AS forward_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng RANGE FOLLOWING cửa sổ frame")
    public void shouldFormatPostgresqlWindowRangeFollowingClause() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at range between current row and 1 following) as rolling_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at
                    RANGE BETWEEN CURRENT ROW AND 1 FOLLOWING
                  ) AS rolling_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng NULLS FIRST Sắp xếp cửa sổ")
    public void shouldFormatPostgresqlWindowNullsFirstClause() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at desc nulls first rows between current row and unbounded following) as forward_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at DESC NULLS FIRST
                    ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
                  ) AS forward_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng EXCLUDE CURRENT ROW cửa sổ frame")
    public void shouldFormatPostgresqlWindowExcludeCurrentRow() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at rows between unbounded preceding and current row exclude current row) as running_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                    EXCLUDE CURRENT ROW
                  ) AS running_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng EXCLUDE TIES cửa sổ frame")
    public void shouldFormatPostgresqlWindowExcludeTies() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at desc nulls last,id asc rows between 1 preceding and 1 following exclude ties) as rolling_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at DESC NULLS LAST,
                    id ASC
                    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
                    EXCLUDE TIES
                  ) AS rolling_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng EXCLUDE GROUP cửa sổ frame")
    public void shouldFormatPostgresqlWindowExcludeGroup() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at groups between 1 preceding and current row exclude group) as grouped_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at
                    GROUPS BETWEEN 1 PRECEDING AND CURRENT ROW
                    EXCLUDE GROUP
                  ) AS grouped_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng EXCLUDE NO OTHERS cửa sổ frame")
    public void shouldFormatPostgresqlWindowExcludeNoOthers() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at desc,id asc rows between 2 preceding and current row exclude no others) as rolling_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at DESC,
                    id ASC
                    ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
                    EXCLUDE NO OTHERS
                  ) AS rolling_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên định dạng các cột được sắp xếp nhiều thứ RANGE cửa sổ frame")
    public void shouldFormatPostgresqlWindowMultiOrderRangeClause() {
        String formatted = SqlFormatter.format(
                "select user_id,sum(total) over (partition by user_id order by created_at desc nulls first,id asc range between current row and unbounded following) as forward_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT user_id,
                  sum(total) OVER (
                    PARTITION BY user_id
                    ORDER BY created_at DESC NULLS FIRST,
                    id ASC
                    RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
                  ) AS forward_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên định dạng nhiều cột PARTITION của EXCLUDE CURRENT ROW")
    public void shouldFormatPostgresqlWindowMultiPartitionExcludeCurrentRow() {
        String formatted = SqlFormatter.format(
                "select team_id,user_id,sum(total) over (partition by team_id,user_id order by created_at desc,id asc rows between unbounded preceding and current row exclude current row) as rolling_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT team_id,
                  user_id,
                  sum(total) OVER (
                    PARTITION BY team_id,
                    user_id
                    ORDER BY created_at DESC,
                    id ASC
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                    EXCLUDE CURRENT ROW
                  ) AS rolling_total
                  FROM orders;""");
    }

    @Test(description = "PostgreSQL Các phương ngữ nên được định dạng FILTER + Nhiều cột PARTITION + RANGE")
    public void shouldFormatPostgresqlFilterWithMultiPartitionRangeWindow() {
        String formatted = SqlFormatter.format(
                "select count(*) filter (where status='paid' and total>100) over (partition by team_id,user_id order by created_at desc nulls first,id asc range between current row and unbounded following exclude no others) as rolling_total from orders",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertEquals(formatted, """
                SELECT count(*) FILTER (
                    WHERE status = 'paid'
                    AND total > 100
                  ) OVER (
                    PARTITION BY team_id,
                    user_id
                    ORDER BY created_at DESC NULLS FIRST,
                    id ASC
                    RANGE BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING
                    EXCLUDE NO OTHERS
                  ) AS rolling_total
                  FROM orders;""");
    }

    @Test(description = "SQL Server Các phương ngữ nên được định dạng UPDATE OUTPUT")
    public void shouldFormatSqlServerUpdateOutput() {
        String formatted = SqlFormatter.format(
                "update [users] set [status]=2 output inserted.[id],inserted.[status] where [status]=1",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertEquals(formatted, """
                UPDATE [users]
                SET [status] = 2
                OUTPUT inserted.[id],
                  inserted.[status]
                WHERE [status] = 1;""");
    }

    @Test(description = "SQL Server Các phương ngữ nên được định dạng UPDATE OUTPUT INTO")
    public void shouldFormatSqlServerUpdateOutputInto() {
        String formatted = SqlFormatter.format(
                "update [users] set [status]=2 output inserted.[id],inserted.[status] into [user_log] where [status]=1",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertEquals(formatted, """
                UPDATE [users]
                SET [status] = 2
                OUTPUT inserted.[id],
                  inserted.[status]
                INTO [user_log]
                WHERE [status] = 1;""");
    }

    @Test(description = "SQL Server Các phương ngữ cần được hỗ trợ TOP và định danh dấu ngoặc vuông")
    public void shouldFormatSqlServerDialectSql() {
        String formatted = SqlFormatter.format("select top 10 [id],[name] from [users] order by [id] desc",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertEquals(formatted, """
                SELECT TOP 10 [id],
                  [name]
                  FROM [users]
                    ORDER BY [id] DESC;""");
    }

    @Test(description = "SQL Server Các phương ngữ nên được định dạng WITH (NOLOCK)")
    public void shouldFormatSqlServerWithNolock() {
        String formatted = SqlFormatter.format(
                "select u.id,u.name from [users] u with (nolock) where u.status=1",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertEquals(formatted, """
                SELECT u.id,
                  u.name
                  FROM [users] u WITH (NOLOCK)
                    WHERE u.status = 1;""");
    }

    @Test(description = "SQL Server Các phương ngữ nên được định dạng OFFSET FETCH")
    public void shouldFormatSqlServerOffsetFetch() {
        String formatted = SqlFormatter.format(
                "select [id],[name] from [users] order by [id] offset 10 rows fetch next 5 rows only",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertEquals(formatted, """
                SELECT [id],
                  [name]
                  FROM [users]
                    ORDER BY [id]
                    OFFSET 10 ROWS
                    FETCH NEXT 5 ROWS ONLY;""");
    }

    @Test(description = "SQL Server Các phương ngữ nên được định dạng CTE Truy vấn")
    public void shouldFormatSqlServerCteQuery() {
        String formatted = SqlFormatter.format(
                "with recent_users as (select top 10 [id],[name],[created_at] from [users] order by [created_at] desc) select [id],[name] from recent_users",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertEquals(formatted, """
                WITH recent_users AS (
                  SELECT TOP 10 [id],
                    [name],
                    [created_at]
                  FROM [users]
                  ORDER BY [created_at] DESC
                )
                SELECT [id],
                  [name]
                  FROM recent_users;""");
    }

    @Test(description = "Oracle Các phương ngữ phải hỗ trợ các mã định danh được trích dẫn kép và  FETCH")
    public void shouldFormatOracleDialectSql() {
        String formatted = SqlFormatter.format("select \"ID\",\"NAME\" from \"USERS\" fetch next 5 rows only",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                SELECT "ID",
                  "NAME"
                  FROM "USERS"
                    FETCH NEXT 5 ROWS ONLY;""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng MERGE tuyên bố")
    public void shouldFormatOracleMergeStatement() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched then update set u.name=t.name,u.status=t.status when not matched then insert (id,name,status) values (t.id,t.name,t.status)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED THEN UPDATE
                SET u.name = t.name,
                  u.status = t.status
                WHEN NOT MATCHED THEN INSERT (id, name, status)
                VALUES (t.id, t.name, t.status);""");
    }

    @Test(description = "Oracle Các phương ngữ cần được bảo tồn MERGE Điều khoản khớp có điều kiện")
    public void shouldFormatOracleMergeMatchedCondition() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched and t.deleted=1 then delete when matched then update set u.name=t.name,u.status=t.status when not matched then insert (id,name,status) values (t.id,t.name,t.status)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED AND t.deleted = 1 THEN DELETE
                WHEN MATCHED THEN UPDATE
                SET u.name = t.name,
                  u.status = t.status
                WHEN NOT MATCHED THEN INSERT (id, name, status)
                VALUES (t.id, t.name, t.status);""");
    }

    @Test(description = "Oracle Các phương ngữ cần được bảo tồn MERGE Mệnh đề chèn có điều kiện")
    public void shouldFormatOracleMergeNotMatchedCondition() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched then update set u.name=t.name when not matched and t.enabled=1 then insert (id,name,status) values (t.id,t.name,t.status)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED THEN UPDATE
                SET u.name = t.name
                WHEN NOT MATCHED AND t.enabled = 1 THEN INSERT (id, name, status)
                VALUES (t.id, t.name, t.status);""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng MERGE Phức tạp INSERT VALUES")
    public void shouldFormatOracleMergeComplexInsertValues() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched then update set u.name=t.name,u.status=t.status when not matched and t.enabled=1 then insert (id,name,status,created_at) values (t.id,t.name,t.status,current_timestamp)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED THEN UPDATE
                SET u.name = t.name,
                  u.status = t.status
                WHEN NOT MATCHED AND t.enabled = 1 THEN INSERT (id, name, status, created_at)
                VALUES (t.id, t.name, t.status, current_timestamp);""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng nhiều lần WHEN MATCHED chi nhánh")
    public void shouldFormatOracleMergeMultipleMatchedBranches() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched and t.deleted=1 then delete when matched and t.status='inactive' then update set u.status=t.status when not matched and t.enabled=1 then insert (id,name,status,created_at) values (t.id,t.name,t.status,current_timestamp)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED AND t.deleted = 1 THEN DELETE
                WHEN MATCHED AND t.status = 'inactive' THEN UPDATE
                SET u.status = t.status
                WHEN NOT MATCHED AND t.enabled = 1 THEN INSERT (id, name, status, created_at)
                VALUES (t.id, t.name, t.status, current_timestamp);""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng MERGE Phức tạp UPDATE SET và INSERT VALUES")
    public void shouldFormatOracleMergeComplexUpdateSetAndInsertValues() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched and t.status='inactive' then update set u.status=t.status,u.updated_at=current_timestamp,u.note='archived' when not matched and t.enabled=1 then insert (id,name,status,created_at,updated_at) values (t.id,t.name,t.status,current_timestamp,current_timestamp)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED AND t.status = 'inactive' THEN UPDATE
                SET u.status = t.status,
                  u.updated_at = current_timestamp,
                  u.note = 'archived'
                WHEN NOT MATCHED AND t.enabled = 1 THEN INSERT (id, name, status, created_at, updated_at)
                VALUES (t.id, t.name, t.status, current_timestamp, current_timestamp);""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng MERGE DELETE WHERE khoản")
    public void shouldFormatOracleMergeDeleteWhereClause() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched then update set u.status=t.status delete where u.status='DELETED' when not matched then insert (id,name,status) values (t.id,t.name,t.status)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED THEN UPDATE
                SET u.status = t.status
                DELETE WHERE u.status = 'DELETED'
                WHEN NOT MATCHED THEN INSERT (id, name, status)
                VALUES (t.id, t.name, t.status);""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng MERGE UPDATE WHERE DELETE WHERE sự kết hợp")
    public void shouldFormatOracleMergeUpdateWhereDeleteWhereClause() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when matched then update set u.status=t.status,u.updated_at=current_timestamp where t.enabled=1 delete where u.status='DELETED' when not matched then insert (id,name,status) values (t.id,t.name,t.status)",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN MATCHED THEN UPDATE
                SET u.status = t.status,
                  u.updated_at = current_timestamp
                WHERE t.enabled = 1
                DELETE WHERE u.status = 'DELETED'
                WHEN NOT MATCHED THEN INSERT (id, name, status)
                VALUES (t.id, t.name, t.status);""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng MERGE INSERT WHERE sự kết hợp")
    public void shouldFormatOracleMergeInsertWhereClause() {
        String formatted = SqlFormatter.format(
                "merge into users u using temp_users t on (u.id=t.id) when not matched then insert (id,name,status,created_at) values (t.id,t.name,t.status,current_timestamp) where t.enabled=1 and t.status<>'DELETED'",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                MERGE INTO users u
                USING temp_users t
                ON (u.id = t.id)
                WHEN NOT MATCHED THEN INSERT (id, name, status, created_at)
                VALUES (t.id, t.name, t.status, current_timestamp)
                WHERE t.enabled = 1
                  AND t.status <> 'DELETED';""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng FETCH FIRST")
    public void shouldFormatOracleFetchFirst() {
        String formatted = SqlFormatter.format(
                "select \"ID\",\"NAME\" from \"USERS\" order by \"ID\" fetch first 5 rows only",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                SELECT "ID",
                  "NAME"
                  FROM "USERS"
                    ORDER BY "ID"
                    FETCH FIRST 5 ROWS ONLY;""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng START WITH và CONNECT BY")
    public void shouldFormatOracleHierarchyQuery() {
        String formatted = SqlFormatter.format(
                "select id,parent_id,name from departments start with parent_id is null connect by prior id = parent_id order siblings by name",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                SELECT id,
                  parent_id,
                  name
                  FROM departments
                    START WITH parent_id IS NULL
                      CONNECT BY PRIOR id = parent_id
                      ORDER SIBLINGS BY name;""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng CONNECT BY NOCYCLE")
    public void shouldFormatOracleConnectByNocycle() {
        String formatted = SqlFormatter.format(
                "select id,parent_id,name from departments start with parent_id is null connect by nocycle prior id = parent_id order siblings by name",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                SELECT id,
                  parent_id,
                  name
                  FROM departments
                    START WITH parent_id IS NULL
                      CONNECT BY NOCYCLE PRIOR id = parent_id
                      ORDER SIBLINGS BY name;""");
    }

    @Test(description = "Oracle Các phương ngữ cần được bảo tồn CONNECT_BY_ROOT")
    public void shouldFormatOracleConnectByRoot() {
        String formatted = SqlFormatter.format(
                "select connect_by_root id as root_id,id,parent_id from departments start with parent_id is null connect by prior id = parent_id",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                SELECT CONNECT_BY_ROOT id AS root_id,
                  id,
                  parent_id
                  FROM departments
                    START WITH parent_id IS NULL
                      CONNECT BY PRIOR id = parent_id;""");
    }

    @Test(description = "Oracle Phương ngữ nên có ở đó MINUS Sau đó bọc sang dòng tiếp theo SELECT")
    public void shouldFormatOracleMinusQuery() {
        String formatted = SqlFormatter.format(
                "select id,name from active_users minus select id,name from blocked_users",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                SELECT id,
                  name
                  FROM active_users
                  MINUS
                SELECT id,
                  name
                  FROM blocked_users;""");
    }

    @Test(description = "MySQL Các phương ngữ nên được định dạng IGNORE INDEX và LOCK IN SHARE MODE")
    public void shouldFormatMysqlIgnoreIndexAndShareLock() {
        String formatted = SqlFormatter.format(
                "select u.id,u.name from users u ignore index(idx_users_status) where u.status=1 lock in share mode",
                SqlFormatter.SqlDialect.MYSQL);

        assertEquals(formatted, """
                SELECT u.id,
                  u.name
                  FROM users u IGNORE INDEX (idx_users_status)
                    WHERE u.status = 1
                    LOCK IN SHARE MODE;""");
    }

    @Test(description = "Oracle Các phương ngữ nên được định dạng thành nhiều lớp MINUS")
    public void shouldFormatOracleMinusChain() {
        String formatted = SqlFormatter.format(
                "select id,name from active_users minus select id,name from blocked_users minus select id,name from archived_users",
                SqlFormatter.SqlDialect.ORACLE);

        assertEquals(formatted, """
                SELECT id,
                  name
                  FROM active_users
                  MINUS
                SELECT id,
                  name
                  FROM blocked_users
                  MINUS
                SELECT id,
                  name
                  FROM archived_users;""");
    }

    @Test(description = "Oracle Việc xác minh phương ngữ nên được cho phép FETCH NEXT")
    public void shouldValidateOracleFetchNext() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate(
                "select \"ID\" from \"USERS\" fetch next 5 rows only",
                SqlFormatter.SqlDialect.ORACLE);

        assertTrue(result.valid(), result.firstIssue());
    }

    @Test(description = "Việc xác minh phương ngữ nên được cho phép PostgreSQL RETURNING")
    public void shouldValidatePostgresqlReturning() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate(
                "insert into users(id,name) values(1,'alice') returning id",
                SqlFormatter.SqlDialect.POSTGRESQL);

        assertTrue(result.valid(), result.firstIssue());
    }

    @Test(description = "Việc xác minh phương ngữ nên được cho phép SQL Server TOP")
    public void shouldValidateSqlServerTop() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate(
                "select top 5 [id] from [users] order by [id] desc",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertTrue(result.valid(), result.firstIssue());
    }

    @Test(description = "SQL Server Các phương ngữ nên hỗ trợ các hình thức trong ngoặc TOP")
    public void shouldValidateSqlServerTopWithParentheses() {
        SqlFormatter.ValidationResult result = SqlFormatter.validate(
                "select top (5) [id] from [users]",
                SqlFormatter.SqlDialect.SQLSERVER);

        assertTrue(result.valid(), result.firstIssue());
    }

    private void assertFormattedEquals(String expected, String input) {
        assertEquals(SqlFormatter.format(input), expected);
    }
}
