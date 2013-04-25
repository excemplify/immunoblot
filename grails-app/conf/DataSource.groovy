/* ===================================================
 * Copyright 2010-2013 HITS gGmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ========================================================== */

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}
// environment specific settings
environments {
    development {//you need to modify the set for your own database
        dataSource {
            dbCreate = "update"
            driverClassName = "com.mysql.jdbc.Driver"
            dialect = org.hibernate.dialect.MySQL5InnoDBDialect
            url = "jdbc:mysql://xx.xx.xx.xx:3306/yourtestdatabase"    
            username = "xx"
            password = "xx"
            pooled = true
            properties{  //org.apache.commons.dbcp.BasicDataSource
                maxActive = 50
                maxIdle = 25
                minIdle = 1
                initialSize = 1
                minEvictableIdleTimeMillis = 60000
                timeBetweenEvictionRunsMillis = 60000
                numTestsPerEvictionRun = 3
                maxWait = 10000

                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = true

                validationQuery = "select 1"
            }
            
            
        }
    }
  
    production {//
        dataSource {
            dbCreate = "update"
            driverClassName = "com.mysql.jdbc.Driver"
            dialect = org.hibernate.dialect.MySQL5InnoDBDialect
            url = "jdbc:mysql://xx.xx.xx.xx:3306/your database"    
            username = "xx"
            password = "xx"
            pooled = true
            properties{  //org.apache.commons.dbcp.BasicDataSource
                maxActive = 50
                maxIdle = 25
                minIdle = 1
                initialSize = 1
                minEvictableIdleTimeMillis = 60000
                timeBetweenEvictionRunsMillis = 60000
                numTestsPerEvictionRun = 3
                maxWait = 10000

                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = true

                validationQuery = "select 1"
            }
            
            
        }
            
            
        }
    
    test {
           dataSource {  //in-memory database might has problem 
                    dbCreate = "create-drop"
                    url = "jdbc:h2:mem:devDb"
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                    pooled = true
                    properties{  //org.apache.commons.dbcp.BasicDataSource
                        maxActive = 50
                        maxIdle = 25
                        minIdle = 1
                        initialSize = 1
                        minEvictableIdleTimeMillis = 60000
                        timeBetweenEvictionRunsMillis = 60000
                        numTestsPerEvictionRun = 3
                        maxWait = 10000
        
                        testOnBorrow = true
                        testWhileIdle = true
                        testOnReturn = true
        
                        validationQuery = "select 1"
                    }
      }

    }
}
