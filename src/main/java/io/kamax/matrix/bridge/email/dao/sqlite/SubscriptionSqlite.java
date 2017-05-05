package io.kamax.matrix.bridge.email.dao.sqlite;


import io.kamax.matrix.bridge.email.config.dao.SubscriptionSqliteConfig;
import io.kamax.matrix.bridge.email.dao.BridgeSubscriptionDao;
import io.kamax.matrix.bridge.email.dao._SubscriptionDao;
import io.kamax.matrix.bridge.email.exception.StorageException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class SubscriptionSqlite implements InitializingBean, _SubscriptionDao {

    private Logger log = LoggerFactory.getLogger(SubscriptionSqlite.class);

    @Autowired
    private SubscriptionSqliteConfig cfg;

    private Connection conn;

    @Override
    public void afterPropertiesSet() throws Exception {
        File dbFile = new File(StringUtils.defaultIfBlank(cfg.getLocation(), "as-subscriptions.db")).getAbsoluteFile();
        log.info("SQLite DB: {}", dbFile.getPath());

        if (!dbFile.exists()) {
            log.info("SQLlite file does not exist, provisioning fresh DB");
        }

        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subscription (id string PRIMARY KEY, email string, threadId string, mxId string, roomId string)");
        }
    }

    @Override
    public void store(BridgeSubscriptionDao dao) {
        log.info("Storing subscription {} in DB", dao.getSubId());

        try (PreparedStatement stmt = conn.prepareStatement("REPLACE INTO subscription VALUES(?,?,?,?,?)")) {
            stmt.setString(1, dao.getSubId());
            stmt.setString(2, dao.getEmail());
            stmt.setString(3, dao.getThreadId());
            stmt.setString(4, dao.getMxId());
            stmt.setString(5, dao.getRoomId());

            int rowCount = stmt.executeUpdate();
            log.info("Updated rows: {}", rowCount);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void delete(String id) {
        log.info("Deleting subscription {} from DB", id);

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM subscription WHERE id = ?")) {
            stmt.setString(1, id);
            int rowCount = stmt.executeUpdate();
            log.info("Updated rows: {}", rowCount);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<BridgeSubscriptionDao> list() {
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rSet = stmt.executeQuery("SELECT * FROM subscription")) {
                List<BridgeSubscriptionDao> daoList = new ArrayList<>();

                while (rSet.next()) {
                    BridgeSubscriptionDao dao = new BridgeSubscriptionDao();
                    dao.setSubId(rSet.getString("id"));
                    dao.setEmail(rSet.getString("email"));
                    dao.setThreadId(rSet.getString("threadId"));
                    dao.setMxId(rSet.getString("mxId"));
                    dao.setRoomId(rSet.getString("roomId"));
                    daoList.add(dao);
                }

                return daoList;
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

}
