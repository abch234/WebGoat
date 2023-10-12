package testcode.sqli;

import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import java.sql.*;
import java.util.ArrayList;

public class SpringPreparedStatementCreatorFactory {
    @GetMapping("/api/foos")
    @ResponseBody
    public void queryUnsafe(@RequestParam String input) {
        String sql = "select * from Users where name = '" + input + "' id=?";
        // ruleid:spring-sqli-deepsemgrep
        new PreparedStatementCreatorFactory(sql);
        // ruleid:spring-sqli-deepsemgrep
        new PreparedStatementCreatorFactory(sql, new int[] {Types.INTEGER});
        // ruleid:spring-sqli-deepsemgrep
        new PreparedStatementCreatorFactory(sql, new ArrayList<SqlParameter>());
    }
}

public class SpringJdbcTemplate {
    JdbcTemplate jdbcTemplate;

    @Autowired
	public initTemplate(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

    @GetMapping("/api/foos2")
    @ResponseBody
    public void query1(@RequestParam String input) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.execute("select * from Users where name = '"+input+"'");
    }

    @GetMapping("/api/foos3")
    @ResponseBody
    public void query2(@RequestParam String input) throws DataAccessException {
        String sql = "select * from Users where name = '" + input + "'";
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.execute(sql);
    }

    @GetMapping("/api/foos4")
    @ResponseBody
    public void query3(@RequestParam String input) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.execute(String.format("select * from Users where name = '%s'",input));
    }

    @GetMapping("/api/foos5")
    @ResponseBody
    public void query4(@RequestParam(required=false) String input) throws DataAccessException {
        String sql = "select * from Users where name = '%s'";
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.execute(String.format(sql,input));
    }

    @GetMapping("/api/foos6")
    @ResponseBody
    public void querySafe(@RequestParam String input) throws DataAccessException {
        String sql = "select * from Users where name = '1'";
        // ok:spring-sqli-deepsemgrep
        jdbcTemplate.execute(sql);
    }

    @GetMapping("/api/foos7")
    @ResponseBody
    public void queryExecute(@RequestParam String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.execute(sql);
        // ok:spring-sqli-deepsemgrep
        jdbcTemplate.execute(new StoredProcCall(sql), new TestCallableStatementCallback());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.execute(sql, (PreparedStatementCallback) new TestCallableStatementCallback());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.execute(sql, new TestCallableStatementCallback());
    }

    @GetMapping("/api/foos8")
    @ResponseBody
    public void queryBatchUpdate(@RequestParam String sql, @RequestParam(required=true) String taintedString) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate(sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate(sql, sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate("select * from dual", sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate(sql, "select * from dual");
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate(sql, new TestBatchPreparedStatementSetter());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate(sql, new ArrayList<UserEntity>(), 11, new TestParameterizedPreparedStatementSetter());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate(sql, new ArrayList<Object[]>());

        // ok:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate("SELECT foo FROM bar WHERE baz = 'biz'", new ArrayList<Object[]>(Arrays.asList(new Object[] {taintedString}));
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.batchUpdate(sql, new ArrayList<Object[]>(), new int[]{Types.INTEGER, Types.VARCHAR, Types.VARCHAR});
    }

    @GetMapping("/api/foos9")
    @ResponseBody
    public void queryForObject(@RequestParam String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, new TestRowMapper());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, new TestRowMapper(), "", "");
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, UserEntity.class);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, UserEntity.class, "", "");
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, new Object[0], UserEntity.class);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, new Object[0], new int[]{Types.INTEGER, Types.VARCHAR, Types.VARCHAR}, UserEntity.class);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, new Object[0], new int[]{Types.INTEGER, Types.VARCHAR, Types.VARCHAR}, new TestRowMapper());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForObject(sql, new Object[0], new TestRowMapper());
    }

    @GetMapping("/api/foos10")
    @ResponseBody
    public void querySamples(@RequestParam String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new TestResultSetExtractor());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new TestRowCallbackHandler());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new TestRowMapper());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new TestPreparedStatementSetter(), new TestResultSetExtractor());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new TestPreparedStatementSetter(), new TestRowCallbackHandler());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new TestPreparedStatementSetter(), new TestRowMapper());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new Object[0], new TestRowMapper());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new Object[0], new TestRowCallbackHandler());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new Object[0], new TestResultSetExtractor());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new Object[0], new int[]{Types.VARCHAR}, new TestResultSetExtractor());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new Object[0], new int[]{Types.VARCHAR}, new TestRowMapper());
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.query(sql, new Object[0], new int[]{Types.VARCHAR}, new TestRowCallbackHandler());
    }

    @GetMapping("/api/foos11")
    @ResponseBody
    public void queryForList(@RequestParam(value='test') String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForList(sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForList(sql, UserEntity.class);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForList(sql, new Object[0], UserEntity.class);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForList(sql, new Object[0], new int[]{Types.VARCHAR});
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForList(sql, new Object[0], new int[]{Types.VARCHAR}, UserEntity.class);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForList(sql, new Object[0]);
    }

    @GetMapping("/api/foos12")
    @ResponseBody
    public void queryForMap(@RequestParam String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForMap(sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForMap(sql, new Object[0]);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForMap(sql, new Object[0], new int[]{Types.VARCHAR});
    }

    @GetMapping("/api/foos13")
    @ResponseBody
    public void queryForRowSet(@RequestParam String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForRowSet(sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForRowSet(sql, new Object[0]);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForRowSet(sql, new Object[0], new int[]{Types.VARCHAR});
    }

    @GetMapping("/api/foos14")
    @ResponseBody
    public void queryForInt(@RequestParam String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForInt(sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForInt(sql, new Object[0]);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForInt(sql, new Object[0], new int[]{Types.VARCHAR});
    }

    @GetMapping("/api/foos15")
    @ResponseBody
    public void queryForLong(@RequestParam String sql) throws DataAccessException {
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForLong(sql);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForLong(sql, new Object[0]);
        // ruleid:spring-sqli-deepsemgrep
        jdbcTemplate.queryForLong(sql, new Object[0], new int[]{Types.VARCHAR});
    }

}

public class SpringBatchUpdateUtils {

    JdbcOperations jdbcOperations;

    @GetMapping("/api/foos16")
    @ResponseBody
    public void queryBatchUpdateUnsafe(@RequestParam String input) {
        String sql = "UPDATE Users SET name = '"+input+"' where id = 1";
        // ruleid:spring-sqli-deepsemgrep
        BatchUpdateUtils.executeBatchUpdate(sql, new ArrayList<Object[]>(),new int[] {Types.INTEGER}, jdbcOperations);
    }

    @GetMapping("/api/foos17")
    @ResponseBody
    public void queryBatchUpdateSafe() {
        String sql = "UPDATE Users SET name = 'safe' where id = 1";
        // ok:spring-sqli-deepsemgrep
        BatchUpdateUtils.executeBatchUpdate(sql, new ArrayList<Object[]>(),new int[] {Types.INTEGER}, jdbcOperations);
    }

    @GetMapping("/api/foos18")
    @ResponseBody
    public void queryNamedParamBatchUpdateUnsafe(@RequestParam String input) {
        String sql = "UPDATE Users SET name = '"+input+"' where id = 1";
        // ruleid:spring-sqli-deepsemgrep
        NamedParameterBatchUpdateUtils.executeBatchUpdate(sql, new ArrayList<Object[]>(),new int[] {Types.INTEGER}, jdbcOperations);
    }


    @GetMapping("/api/foos18b")
    @ResponseBody
    public void queryNamedParamBatchUpdateUnsafe(@RequestParam String input) {
        String sql = "UPDATE Users SET name = '"+(input != null)+"' where id = 1";
        // ok:spring-sqli-deepsemgrep
        NamedParameterBatchUpdateUtils.executeBatchUpdate(sql, new ArrayList<Object[]>(),new int[] {Types.INTEGER}, jdbcOperations);
    }

    @GetMapping("/api/foos19")
    @ResponseBody
    public void queryNamedParameterBatchUpdateUtilsSafe() {
        String sql = "UPDATE Users SET name = 'safe' where id = 1";
        // ok:spring-sqli-deepsemgrep
        NamedParameterBatchUpdateUtils.executeBatchUpdate(sql, new ArrayList<Object[]>(), new int[]{Types.INTEGER}, jdbcOperations);
    }

    @GetMapping("/api/foos20")
    @ResponseBody
    public void queryNamedParamBatchUpdateOk1(@RequestParam Boolean input) {
        String sql = "UPDATE Users SET name = '"+input+"' where id = 1";
        // ok:spring-sqli-deepsemgrep
        NamedParameterBatchUpdateUtils.executeBatchUpdate(sql, new ArrayList<Object[]>(),new int[] {Types.INTEGER}, jdbcOperations);
    }

    @GetMapping("/api/foos21")
    @ResponseBody
    public void queryNamedParamBatchUpdateOk2(@RequestParam String input) {
        String sql = "UPDATE Users SET name = '"+ (input.length() == 10) +"' where id = 1";
        // ok:spring-sqli-deepsemgrep
        NamedParameterBatchUpdateUtils.executeBatchUpdate(sql, new ArrayList<Object[]>(),new int[] {Types.INTEGER}, jdbcOperations);
    }
}

