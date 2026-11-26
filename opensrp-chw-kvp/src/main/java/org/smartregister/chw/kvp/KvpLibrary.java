package org.smartregister.chw.kvp;

import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitDetailsRepository;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.repository.Repository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;

import id.zelory.compressor.Compressor;

public class KvpLibrary {
    public interface FamilyPlanningExemption {
        boolean appliesTo(Visit visit, String gender);
    }

    public interface CervicalScreeningExemption {
        boolean appliesTo(Visit visit, String gender);
    }

    private static KvpLibrary instance;

    private final Context context;
    private final Repository repository;
    private final FamilyPlanningExemption familyPlanningExemption;
    private final CervicalScreeningExemption cervicalScreeningExemption;

    private int applicationVersion;
    private int databaseVersion;
    private ECSyncHelper syncHelper;

    private ClientProcessorForJava clientProcessorForJava;
    private Compressor compressor;

    private VisitRepository visitRepository;
    private VisitDetailsRepository visitDetailsRepository;
    public String getSaveDateFormat() {
        return saveDateFormat;
    }

    public void setSaveDateFormat(String saveDateFormat) {
        this.saveDateFormat = saveDateFormat;
    }

    private String sourceDateFormat = "dd-MM-yyyy";
    private String saveDateFormat = "yyyy-MM-dd";

    public static boolean isSubmitOnSave() {
        return submitOnSave;
    }

    public void setSubmitOnSave(boolean submitOnSave) {
        KvpLibrary.submitOnSave = submitOnSave;
    }

    private static boolean submitOnSave = false;

    public String getSourceDateFormat() {
        return sourceDateFormat;
    }

    public void setSourceDateFormat(String sourceDateFormat) {
        this.sourceDateFormat = sourceDateFormat;
    }


    private KvpLibrary(Context contextArg, Repository repositoryArg, int applicationVersion, int databaseVersion,
                       FamilyPlanningExemption familyPlanningExemption,
                       CervicalScreeningExemption cervicalScreeningExemption) {
        this.context = contextArg;
        this.repository = repositoryArg;
        this.applicationVersion = applicationVersion;
        this.databaseVersion = databaseVersion;
        this.familyPlanningExemption = familyPlanningExemption;
        this.cervicalScreeningExemption = cervicalScreeningExemption;
    }

    public static void init(Context context, Repository repository, int applicationVersion, int databaseVersion) {
        init(context, repository, applicationVersion, databaseVersion, (visit, gender) -> false);
    }

    public static void init(Context context, Repository repository, int applicationVersion, int databaseVersion,
                            FamilyPlanningExemption familyPlanningExemption) {
        init(context, repository, applicationVersion, databaseVersion, familyPlanningExemption, (visit, gender) -> false);
    }

    public static void init(Context context, Repository repository, int applicationVersion, int databaseVersion,
                            FamilyPlanningExemption familyPlanningExemption,
                            CervicalScreeningExemption cervicalScreeningExemption) {
        if (instance == null) {
            instance = new KvpLibrary(context, repository, applicationVersion, databaseVersion,
                    familyPlanningExemption, cervicalScreeningExemption);
        }
    }

    public boolean isFamilyPlanningExempt(Visit visit, String gender) {
        return familyPlanningExemption.appliesTo(visit, gender);
    }

    public boolean isCervicalScreeningExempt(Visit visit, String gender) {
        return cervicalScreeningExemption.appliesTo(visit, gender);
    }

    public static KvpLibrary getInstance() {
        if (instance == null) {
            throw new IllegalStateException(" Instance does not exist!!! Call "
                    + CoreLibrary.class.getName()
                    + ".init method in the onCreate method of "
                    + "your Application class ");
        }
        return instance;
    }

    public Context context() {
        return context;
    }

    public Repository getRepository() {
        return repository;
    }

    public int getApplicationVersion() {
        return applicationVersion;
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    public ECSyncHelper getEcSyncHelper() {
        if (syncHelper == null) {
            syncHelper = ECSyncHelper.getInstance(context().applicationContext());
        }
        return syncHelper;
    }

    public ClientProcessorForJava getClientProcessorForJava() {
        if (clientProcessorForJava == null) {
            clientProcessorForJava = ClientProcessorForJava.getInstance(context().applicationContext());
        }
        return clientProcessorForJava;
    }

    public void setClientProcessorForJava(ClientProcessorForJava clientProcessorForJava) {
        this.clientProcessorForJava = clientProcessorForJava;
    }

    public VisitRepository visitRepository() {
        if (visitRepository == null) {
            visitRepository = new VisitRepository();
        }
        return visitRepository;
    }

    public VisitDetailsRepository visitDetailsRepository() {
        if (visitDetailsRepository == null) {
            visitDetailsRepository = new VisitDetailsRepository();
        }
        return visitDetailsRepository;
    }

}
