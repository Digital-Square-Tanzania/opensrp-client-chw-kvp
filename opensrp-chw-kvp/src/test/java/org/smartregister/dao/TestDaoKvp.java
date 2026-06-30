package org.smartregister.dao;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.repository.Repository;

@RunWith(MockitoJUnitRunner.class)
public class TestDaoKvp extends KvpDao {

    @Mock
    private Repository repository;

    @Mock
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        setRepository(repository);
    }



    @Test
    public void testIsRegisteredForKvp() {
        Mockito.doReturn(database).when(repository).getReadableDatabase();
        boolean registered = KvpDao.isRegisteredForKvpPrEP("12345");
        Mockito.verify(database).rawQuery(Mockito.anyString(), Mockito.any());
        Assert.assertFalse(registered);
    }
}
