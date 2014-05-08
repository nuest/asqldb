package org.hsqldb.ras.tests;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import org.hsqldb.ras.RasUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by Johannes on 5/5/14.
 *
 * @author Johannes Bachhuber
 */
public class RasTester {

    public static final String DEFAULT_DB_FILE = "/var/hsqldb/db";

    private String dbFile = DEFAULT_DB_FILE;

    private PrintStream out = System.out;

    private boolean testSql;
    private boolean testRas;


    public static void main(String[] args) throws SQLException, JSAPException {
        JSAP jsap = new JSAP();

        FlaggedOption opt1 = new FlaggedOption("dbFile")
                .setStringParser(JSAP.STRING_PARSER)
                .setDefault(DEFAULT_DB_FILE)
                .setRequired(false)
                .setShortFlag('d')
                .setLongFlag("database-file");

        jsap.registerParameter(opt1);

        Switch sw1 = new Switch("sql")
                .setShortFlag('s')
                .setLongFlag("sql");

        jsap.registerParameter(sw1);

        Switch sw2 = new Switch("noras")
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("noras");

        jsap.registerParameter(sw2);

        JSAPResult config = jsap.parse(args);

        RasTester rasTester = new RasTester(config);
        rasTester.test();

        // check whether the command line was valid, and if it wasn't,
        // display usage information and exit.
        if (!config.success()) {
            System.err.println();
            System.err.println("Usage: java ... ");
            System.err.println("                "
                    + jsap.getUsage());
            System.err.println();
            System.exit(1);
        }
    }

    public RasTester(final JSAPResult config) {

        dbFile = config.getString("dbFile");
        testSql = config.getBoolean("sql");
        testRas = !config.getBoolean("noras");
    }

    public void test() {
        boolean success;

        try {
            Connection conn = getConnection();

            success = dropTables(conn);

            //test setup:
            success = success && createTables(conn);
            success = success && insertValues(conn);

            //test queries:
            success = success && runTests(conn);

            //test cleanup:
            success = success && dropTables(conn);
        } catch (final SQLException e) {
            throw new RuntimeException("Tests FAILED. SQLExcpetion occured while performing tests.", e);
        }

        if (success) {
            out.println("All tests PASSED");
        } else {
            out.println("Tests FAILED.");
        }
    }

    public boolean createTables(final Connection conn) throws SQLException {
        String createString =
                "create table RASTEST (ID integer NOT NULL, COLL varchar(40) ARRAY NOT NULL, PRIMARY KEY (ID))";
        return executeQuery(conn, createString, 0);
    }

    public boolean insertValues(final Connection conn) throws SQLException {
        final String oidQuery = "select oid(c) from rgb as c";
        String oid = RasUtil.executeRasqlQuery(oidQuery).toString();
        oid = oid.replaceAll("[\\[\\]]", "");
        String[] insertQueries = new String[]{
                "INSERT INTO RASTEST VALUES(0, ARRAY['rgb:" + Double.valueOf(oid).intValue() + "'])",
                "INSERT INTO RASTEST VALUES(1, ARRAY['rgb:" + Double.valueOf(oid).intValue() + "'])"
        };
        for (String query : insertQueries) {
            if (!executeQuery(conn, query, 0))
                return false;
        }
        return true;
    }

    public boolean runTests(final Connection conn) throws SQLException {
        final String rasqlTests = "testrun/asqldb/mixedQueries.txt";
        final String sqlArithmetic = "testrun/hsqldb/TestSelfArithmetic.txt";
        final String sqlQueries = "testrun/hsqldb/TestSelfQueries.txt";
        final String sqlJoins = "testrun/hsqldb/TestSelfJoins.txt";
        final List<String> testFiles = new ArrayList<String>();
        if (testRas)
            testFiles.add(rasqlTests);
        if (testSql)
            testFiles.addAll(Arrays.asList(sqlArithmetic, sqlQueries, sqlJoins));

        List<String> queries;
        for (String file : testFiles) {
            out.println("\n===========================\n" +
                        " Running queries in "+file+"\n" +
                        "===========================");
            try {
                queries = Files.readAllLines(Paths.get(file), Charset.forName("UTF-8"));
            } catch (IOException e) {
                throw new RuntimeException("File "+file+" could not be opened.", e);
            }
            for (int i = 0; i < queries.size(); i++) {
                String query = queries.get(i);
                if (query.isEmpty() || query.startsWith("-"))
                    continue;
                while(queryHasAnotherLine(query)) {
                    query += queries.get(++i);
                }
                if (!executeQuery(conn, query, i+1))
                    return false;
            }
        }
        return true;
    }

    private boolean queryHasAnotherLine(final String query) {
        switch(query.charAt(query.length()-1)) {
            case '(':
            case ')':
            case ',':
                return true;
        }
        return false;
    }

    public boolean dropTables(final Connection conn) throws SQLException {
        String createString = "drop table if exists RASTEST";
        return executeQuery(conn, createString, 0);
    }

    private boolean executeQuery(final Connection conn, final String query, final int line) throws SQLException{
        out.printf("Executing query on line %d: %s\n... ", line, query);
        final boolean errorExpected = query.startsWith("/*e");
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeQuery(query);
        } catch (SQLException e) {
            if (!errorExpected) {
                out.println("\n>>>> Query failed! <<<<");
                e.printStackTrace();
                return false;
            }
            out.println("Success!");
            return true;
        } finally {
            if (stmt != null) { stmt.close(); }
        }
        if (errorExpected) {
            out.println("\n>>>> Test failed! Query should have given an error, but didn't <<<<");
            return false;
        }
        out.println("Success!");
        return true;
    }

    public Connection getConnection() throws SQLException {

        Connection conn;
        Properties connectionProps = new Properties();
        connectionProps.put("user", "SA");
        connectionProps.put("password", "");

        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load the hsqldb JDBCDriver", e);
        }

        final String jdbcUrl = "jdbc:hsqldb:file:" + dbFile;
        conn = DriverManager.getConnection(
                jdbcUrl,
                connectionProps
        );
        System.out.println("Connected to database: "+jdbcUrl);
        return conn;
    }
}