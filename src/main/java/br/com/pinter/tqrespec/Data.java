/*
 * Copyright (C) 2018 Emerson Pinter - All Rights Reserved
 */

/*    This file is part of TQ Respec.

    TQ Respec is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    TQ Respec is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with TQ Respec.  If not, see <http://www.gnu.org/licenses/>.
*/

package br.com.pinter.tqrespec;

import br.com.pinter.tqdatabase.Database;
import br.com.pinter.tqdatabase.Text;

import java.io.IOException;

public class Data {
    private static volatile Data _instance;
    private Database db;
    private Text text;

    private static Data getInstance() {
        Data d = _instance;
        if (d == null) {
            synchronized (Data.class) {
                d = _instance;
                if (d == null) {
                    d = new Data();
                    _instance = d;
                }
            }
        }
        return _instance;
    }

    private Data() {
        String dbPath = null;
        try {
            dbPath = String.format("%s/Database/database.arz", GameInfo.getInstance().getGamePath());
            db = new Database(dbPath);
            text = new Text(GameInfo.getInstance().getGamePath() + "/Text");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("Error loading database from '%s'", dbPath));
        }
    }

    public static Database db() {
        return getInstance().db;
    }

    public static Text text() {
        return getInstance().text;
    }
}
