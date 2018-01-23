package jtest;

import nvd.data.DBConnection;
import workflow.MainWorkFlow;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by kaimaoyang on 2017/4/19.
 */
public class JTest {

    public static void main(String[] args) throws SQLException {
        Connection conn = DBConnection.getConnection();
        MainWorkFlow mwf = new MainWorkFlow(conn);
        mwf.eval(args[0] + "+" + args[1]);
        conn.close();
    }
}
