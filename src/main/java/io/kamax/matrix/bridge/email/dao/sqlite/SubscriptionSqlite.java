/*
 * matrix-appservice-email - Matrix Bridge to E-mail
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
            log.info("SQLlite DB file does not exist, provisioning");
        }

        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subscription (id string PRIMARY KEY, sourceMxId string, timestamp long, email string, threadId string, mxId string, roomId string)");
        }
    }

    @Override
    public void store(BridgeSubscriptionDao dao) {
        log.info("Storing subscription {} in DB", dao.getSubId());

        try (PreparedStatement stmt = conn.prepareStatement("REPLACE INTO subscription VALUES(?,?,?,?,?,?,?)")) {
            stmt.setString(1, dao.getSubId());
            stmt.setString(2, dao.getSourceMxId());
            stmt.setLong(3, dao.getTimestamp());
            stmt.setString(4, dao.getEmail());
            stmt.setString(5, dao.getThreadId());
            stmt.setString(6, dao.getMxId());
            stmt.setString(7, dao.getRoomId());

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
                    dao.setSourceMxId(rSet.getString("sourceMxId"));
                    dao.setTimestamp(rSet.getLong("timestamp"));
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
