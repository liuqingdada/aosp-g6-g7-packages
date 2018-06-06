package com.mstarc.wearablephone.database.greendao;

import com.mstarc.wearablephone.database.bean.InterceptCall;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import java.util.Map;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig interceptCallDaoConfig;

    private final InterceptCallDao interceptCallDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        interceptCallDaoConfig = daoConfigMap.get(InterceptCallDao.class).clone();
        interceptCallDaoConfig.initIdentityScope(type);

        interceptCallDao = new InterceptCallDao(interceptCallDaoConfig, this);

        registerDao(InterceptCall.class, interceptCallDao);
    }
    
    public void clear() {
        interceptCallDaoConfig.clearIdentityScope();
    }

    public InterceptCallDao getInterceptCallDao() {
        return interceptCallDao;
    }

}
