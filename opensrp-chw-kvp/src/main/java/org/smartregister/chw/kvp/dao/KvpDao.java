package org.smartregister.chw.kvp.dao;

import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.dao.AbstractDao;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class KvpDao extends AbstractDao {


    public static boolean isRegisteredForKvpPrEP(String baseEntityID) {
        String sql = "SELECT count(p.base_entity_id) count FROM ec_kvp_prep_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return false;

        return res.get(0) > 0;
    }

    public static boolean isRegisteredForKvp(String baseEntityID) {
        String sql = "SELECT count(p.base_entity_id) count FROM ec_kvp_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return false;

        return res.get(0) > 0;
    }

    public static boolean isRegisteredForPrEP(String baseEntityID) {
        String sql = "SELECT count(p.base_entity_id) count FROM ec_prep_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return false;

        return res.get(0) > 0;
    }

    public static boolean isClientEligibleForPrEPFromScreening(String baseEntityID){
        String sql = "SELECT prep_qualified FROM ec_kvp_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "prep_qualified");

        List<String> res = readData(sql, dataMap);
        if(res != null && res.size() != 0 && res.get(0)!= null){
            return res.get(0).equalsIgnoreCase("yes");
        }
        return false;
    }

    public static boolean isClientHTSResultsNegative(String baseEntityID){
        String sql = "SELECT hiv_status FROM ec_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "hiv_status");

        List<String> res = readData(sql, dataMap);
        if(res != null && res.size() != 0 && res.get(0)!= null){
            return res.get(0).equalsIgnoreCase("negative");
        }
        return false;
    }

    public static MemberObject getMember(String baseEntityID) {
        String sql = "select m.base_entity_id,\n" +
                "       m.unique_id,\n" +
                "       m.relational_id,\n" +
                "       m.dob,\n" +
                "       m.first_name,\n" +
                "       m.middle_name,\n" +
                "       m.last_name,\n" +
                "       m.gender,\n" +
                "       m.phone_number,\n" +
                "       m.other_phone_number,\n" +
                "       f.first_name     family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       f.village_town,\n" +
                "       fh.first_name    family_head_first_name,\n" +
                "       fh.middle_name   family_head_middle_name,\n" +
                "       fh.last_name     family_head_last_name,\n" +
                "       fh.phone_number  family_head_phone_number,\n" +
                "       ancr.is_closed   anc_is_closed,\n" +
                "       pncr.is_closed   pnc_is_closed,\n" +
                "       pcg.first_name   pcg_first_name,\n" +
                "       pcg.last_name    pcg_last_name,\n" +
                "       pcg.middle_name  pcg_middle_name,\n" +
                "       pcg.phone_number pcg_phone_number,\n" +
                "       mr.*\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "         inner join ec_kvp_prep_register mr on mr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "         left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver\n" +
                "         left join ec_anc_register ancr on ancr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_pregnancy_outcome pncr on pncr.base_entity_id = m.base_entity_id\n" +
                "where m.base_entity_id = '" + baseEntityID + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setKvpTestDate(getCursorValueAsDate(cursor, "kvp_test_date", df));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setAncMember(getCursorValue(cursor, "anc_is_closed", ""));
            memberObject.setPncMember(getCursorValue(cursor, "pnc_is_closed", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName =
                    (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " "
                    + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName =
                    (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }

    public static MemberObject getKvpMember(String baseEntityID) {
        String sql = "select m.base_entity_id,\n" +
                "       m.unique_id,\n" +
                "       m.relational_id,\n" +
                "       m.dob,\n" +
                "       m.first_name,\n" +
                "       m.middle_name,\n" +
                "       m.last_name,\n" +
                "       m.gender,\n" +
                "       m.phone_number,\n" +
                "       m.other_phone_number,\n" +
                "       f.first_name     family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       f.village_town,\n" +
                "       fh.first_name    family_head_first_name,\n" +
                "       fh.middle_name   family_head_middle_name,\n" +
                "       fh.last_name     family_head_last_name,\n" +
                "       fh.phone_number  family_head_phone_number,\n" +
                "       ancr.is_closed   anc_is_closed,\n" +
                "       pncr.is_closed   pnc_is_closed,\n" +
                "       pcg.first_name   pcg_first_name,\n" +
                "       pcg.last_name    pcg_last_name,\n" +
                "       pcg.middle_name  pcg_middle_name,\n" +
                "       pcg.phone_number pcg_phone_number,\n" +
                "       mr.*\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "         inner join ec_kvp_register mr on mr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "         left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver\n" +
                "         left join ec_anc_register ancr on ancr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_pregnancy_outcome pncr on pncr.base_entity_id = m.base_entity_id\n" +
                "where m.base_entity_id = '" + baseEntityID + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setKvpTestDate(getCursorValueAsDate(cursor, "kvp_test_date", df));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setAncMember(getCursorValue(cursor, "anc_is_closed", ""));
            memberObject.setPncMember(getCursorValue(cursor, "pnc_is_closed", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName =
                    (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " "
                    + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName =
                    (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }

    public static MemberObject getPrEPMember(String baseEntityID) {
        String sql = "select m.base_entity_id,\n" +
                "       m.unique_id,\n" +
                "       m.relational_id,\n" +
                "       m.dob,\n" +
                "       m.first_name,\n" +
                "       m.middle_name,\n" +
                "       m.last_name,\n" +
                "       m.gender,\n" +
                "       m.phone_number,\n" +
                "       m.other_phone_number,\n" +
                "       f.first_name     family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       f.village_town,\n" +
                "       fh.first_name    family_head_first_name,\n" +
                "       fh.middle_name   family_head_middle_name,\n" +
                "       fh.last_name     family_head_last_name,\n" +
                "       fh.phone_number  family_head_phone_number,\n" +
                "       ancr.is_closed   anc_is_closed,\n" +
                "       pncr.is_closed   pnc_is_closed,\n" +
                "       pcg.first_name   pcg_first_name,\n" +
                "       pcg.last_name    pcg_last_name,\n" +
                "       pcg.middle_name  pcg_middle_name,\n" +
                "       pcg.phone_number pcg_phone_number,\n" +
                "       mr.*\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "         inner join ec_prep_register mr on mr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "         left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver\n" +
                "         left join ec_anc_register ancr on ancr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_pregnancy_outcome pncr on pncr.base_entity_id = m.base_entity_id\n" +
                "where m.base_entity_id = '" + baseEntityID + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setKvpTestDate(getCursorValueAsDate(cursor, "kvp_test_date", df));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setAncMember(getCursorValue(cursor, "anc_is_closed", ""));
            memberObject.setPncMember(getCursorValue(cursor, "pnc_is_closed", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName =
                    (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " "
                    + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName =
                    (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }

    public static String getUIC_ID(String baseEntityId, String tableName) {
        String sql = "SELECT uic_id FROM " + tableName + " p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "uic_id");

        List<String> res = readData(sql, dataMap);
        if(res != null && res.size() != 0 && res.get(0)!= null){
            return res.get(0);
        }
        return "";
    }

    public static String getDominantKVPGroup(String baseEntityId) {
        String sql = "SELECT client_group FROM ec_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_group");

        List<String> res = readData(sql, dataMap);
        if(res != null && res.size() != 0 && res.get(0)!= null){
            return res.get(0);
        }
        return "";
    }
}
