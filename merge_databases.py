"""
merge_databases.py

Merges three MySQL databases with identical schema (ulee_db) into one target
database, safely remapping auto-increment primary keys and foreign keys so
nothing collides or breaks.

WHY THIS IS NEEDED
-------------------
All three of your local databases have tables like `users` where IDs start
at 1. If we just copied all rows into one database, teammate A's user #1
and teammate B's user #1 would collide. This script instead:

  1. Inserts each teammate's rows one table at a time, in dependency order
     (parents before children).
  2. Lets the target database assign brand-new auto-increment IDs.
  3. Remembers a mapping of "old ID -> new ID" for every table, per source
     database.
  4. Uses that mapping to rewrite foreign key columns before inserting
     child rows, so relationships stay correct after the merge.
  5. Handles the special case where `students`, `landlords`, and `admins`
     reuse `users.userID` as their own primary key (joined-table
     inheritance) -- their "new ID" is forced to match the already-remapped
     userID, not a fresh auto-increment value.

BEFORE YOU RUN THIS
--------------------
1. pip install mysql-connector-python
2. Create an empty target database with the SAME schema (run your
   structure-only dump's CREATE TABLE statements against a fresh database).
3. Fill in SOURCES and TARGET below with real host/port/user/password/db
   for each of your three local databases and the new merged database.
4. This script INSERTS ONLY -- it never drops or modifies your original
   databases. Safe to re-run against a fresh empty target if something
   goes wrong.
5. Read the NOTES section at the bottom about `reviews`, `notifications`,
   `reports`, and `property_panorama` -- their foreign keys aren't
   enforced by MySQL, so double check the FK_COLUMNS mapping matches your
   actual data before trusting the remap for those tables.
"""

import mysql.connector
from mysql.connector import Error

# ---------------------------------------------------------------------------
# 1. CONFIGURE YOUR CONNECTIONS HERE
# ---------------------------------------------------------------------------

SOURCES = [
    {
        "label": "student_dev",
        "host": "127.0.0.1",
        "port": 3306,
        "user": "root",
        "password": "Super1010_",
        "database": "ulee_db",
    },
    {
        "label": "landlord_dev",
        "host": "127.0.0.1",
        "port": 3306,
        "user": "root",
        "password": "Super1010_",
        "database": "ulee_db_ezile",
    },
    {
        "label": "admin_dev",
        "host": "127.0.0.1",
        "port": 3306,
        "user": "root",
        "password": "Super1010_",
        "database": "ulee_db_unakho",
    },
]

TARGET = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "root",
    "password": "Super1010_",
    "database": "ulee_db_merged",
}

# ---------------------------------------------------------------------------
# 2. TABLE MERGE ORDER (parents before children -- do not reorder lightly)
# ---------------------------------------------------------------------------

MERGE_ORDER = [
    "users",
    "students",
    "landlords",
    "admins",
    "adminnotificationreads",
    "amenity",
    "property",
    "property_amenity",
    "property_feature",
    "property_feature_image",
    "propertyimage",
    "property_panorama",
    "panorama_hotspot",
    "application",
    "application_document",
    "checklistitem",
    "review",
    "reviews",
    "savedproperty",
    "searchfilter",
    "inquiry",
    "message",
    "notifications",
    "report",
    "reports",
]

# ---------------------------------------------------------------------------
# 3. PER-TABLE CONFIG
#
# pk: the primary key column name.
# shares_pk_with: if set, this table's new PK is forced to equal the
#     already-remapped ID from that other table (joined-table inheritance),
#     instead of getting a fresh auto-increment value.
# fk_columns: {column_name: table_that_owns_the_id_it_points_to}
#     Used to rewrite foreign key values using that table's ID map.
#     Include columns even if MySQL doesn't enforce the FK (e.g. reviews,
#     notifications, reports) as long as they really do reference that ID.
# ---------------------------------------------------------------------------

TABLES = {
    "users": {
        "pk": "userID",
        "fk_columns": {},
    },
    "students": {
        "pk": "studentID",
        "shares_pk_with": ("users", "userID"),
        "fk_columns": {},
    },
    "landlords": {
        "pk": "landlordID",
        "shares_pk_with": ("users", "userID"),
        "fk_columns": {},
    },
    "admins": {
        "pk": "adminID",
        "shares_pk_with": ("users", "userID"),
        "fk_columns": {},
    },
    "adminnotificationreads": {
        "pk": "id",
        "fk_columns": {},
    },
    "amenity": {
        "pk": "amenityID",
        "fk_columns": {},
    },
    "property": {
        "pk": "propertyID",
        "fk_columns": {"landlordID": "landlords"},
    },
    "property_amenity": {
        "pk": None,  # composite key, no auto-increment surrogate
        "fk_columns": {"propertyID": "property", "amenityID": "amenity"},
    },
    "property_feature": {
        "pk": "featureID",
        "fk_columns": {"propertyID": "property"},
    },
    "property_feature_image": {
        "pk": "imageID",
        "fk_columns": {"featureID": "property_feature"},
    },
    "propertyimage": {
        "pk": "imageID",
        "fk_columns": {"propertyID": "property"},
    },
    "property_panorama": {
        "pk": "panoramaID",
        # NOTE: not enforced by MySQL. Confirm 'imageID' really refers to
        # propertyimage.imageID before trusting this -- if it's unused or
        # refers to something else, remove it from fk_columns.
        "fk_columns": {"propertyID": "property", "imageID": "propertyimage"},
    },
    "panorama_hotspot": {
        "pk": "hotspotID",
        # NOTE: not enforced by MySQL.
        "fk_columns": {
            "sourcePanoramaID": "property_panorama",
            "targetPanoramaID": "property_panorama",
        },
    },
    "application": {
        "pk": "applicationID",
        "fk_columns": {"studentID": "students", "propertyID": "property"},
    },
    "application_document": {
        "pk": "documentID",
        "fk_columns": {"applicationID": "application"},
    },
    "checklistitem": {
        "pk": "checklistID",
        "fk_columns": {"studentID": "students"},
    },
    "review": {
        "pk": "reviewID",
        "fk_columns": {"studentID": "students", "propertyID": "property"},
    },
    "reviews": {
        "pk": "reviewID",
        # NOTE: not enforced by MySQL, but same shape as `review`.
        "fk_columns": {"studentID": "students", "propertyID": "property"},
    },
    "savedproperty": {
        "pk": "savedID",
        "fk_columns": {"studentID": "students", "propertyID": "property"},
    },
    "searchfilter": {
        "pk": "searchID",
        "fk_columns": {"studentID": "students"},
    },
    "inquiry": {
        "pk": "inquiryID",
        "fk_columns": {"adminID": "admins"},
    },
    "message": {
        "pk": "messageID",
        "fk_columns": {},
    },
    "notifications": {
        "pk": "notificationID",
        # NOTE: not enforced by MySQL. landlordID looks like it points at
        # landlords.landlordID (== users.userID); confirm before trusting.
        "fk_columns": {"landlordID": "landlords", "propertyID": "property"},
    },
    "report": {
        "pk": "reportID",
        # reporterID appears to be the user who submitted the report.
        "fk_columns": {"propertyID": "property", "reporterID": "users"},
    },
    "reports": {
        "pk": "reportID",
        # NOTE: not enforced by MySQL. staffID likely points at admins
        # (== users.userID) -- confirm before trusting.
        "fk_columns": {
            "propertyID": "property",
            "staffID": "admins",
            "studentID": "students",
        },
    },
}


# ---------------------------------------------------------------------------
# 4. MERGE LOGIC -- you shouldn't need to edit below this line
# ---------------------------------------------------------------------------

def connect(cfg):
    return mysql.connector.connect(
        host=cfg["host"],
        port=cfg["port"],
        user=cfg["user"],
        password=cfg["password"],
        database=cfg["database"],
    )


def get_columns(cur, database, table):
    """Return target/source column metadata keyed by column name."""
    cur.execute(
        """
        SELECT COLUMN_NAME, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
        FROM information_schema.columns
        WHERE table_schema = %s AND table_name = %s
        ORDER BY ORDINAL_POSITION
        """,
        (database, table),
    )
    return {row["COLUMN_NAME"]: row for row in cur.fetchall()}


def target_has_data(cur):
    """True if any merge table in the target already contains rows."""
    for table in MERGE_ORDER:
        cur.execute(
            """
            SELECT COUNT(*) AS cnt
            FROM information_schema.tables
            WHERE table_schema = %s AND table_name = %s
            """,
            (TARGET["database"], table),
        )
        if cur.fetchone()["cnt"] == 0:
            continue
        cur.execute(f"SELECT 1 FROM `{table}` LIMIT 1")
        if cur.fetchone():
            return True
    return False


def find_existing_exact(cur, table, row, pk):
    """
    Find an exact existing target row using every insertable non-PK column.
    This safely collapses identical baseline/seed rows copied between teammates.
    """
    compare_cols = [c for c in row.keys() if c != pk]
    if not compare_cols:
        return None

    where_parts = []
    params = []
    for col in compare_cols:
        val = row[col]
        if val is None:
            where_parts.append(f"`{col}` IS NULL")
        else:
            where_parts.append(f"`{col}` <=> %s")
            params.append(val)

    sql = (
        f"SELECT `{pk}` FROM `{table}` "
        f"WHERE {' AND '.join(where_parts)} LIMIT 1"
    )
    cur.execute(sql, params)
    found = cur.fetchone()
    return found[pk] if found else None


def find_existing_by_natural_key(cur, table, row, pk):
    """
    Map common shared seed/reference rows instead of treating UNIQUE clashes
    as merge failures.
    """
    natural_keys = {
        "users": ["email"],
        "amenity": ["name"],
        "adminnotificationreads": ["eventKey"],
    }
    keys = natural_keys.get(table)
    if not keys or any(k not in row or row[k] is None for k in keys):
        return None

    where = " AND ".join(f"`{k}` <=> %s" for k in keys)
    cur.execute(
        f"SELECT `{pk}` FROM `{table}` WHERE {where} LIMIT 1",
        [row[k] for k in keys],
    )
    found = cur.fetchone()
    return found[pk] if found else None


def prepare_row_for_target(table, row, target_columns):
    """
    Keep only columns that exist in the target schema.

    If a source explicitly contains NULL for a target NOT NULL column that has
    a database default, omit that column so MySQL applies the target default.
    This handles schema drift such as property.capacity becoming NOT NULL
    DEFAULT 1.
    """
    cleaned = {}
    dropped = []

    for col, val in row.items():
        meta = target_columns.get(col)
        if meta is None:
            dropped.append(col)
            continue

        if (
            val is None
            and meta["IS_NULLABLE"] == "NO"
            and meta["COLUMN_DEFAULT"] is not None
        ):
            # Omit it; MySQL will use the target-side default.
            continue

        cleaned[col] = val

    return cleaned, dropped


def merge():
    target_conn = connect(TARGET)
    target_conn.autocommit = False
    target_cur = target_conn.cursor(dictionary=True)

    # The merge mapping is only reliable from a fresh target.
    if target_has_data(target_cur):
        target_cur.close()
        target_conn.close()
        raise RuntimeError(
            f"Target database '{TARGET['database']}' is not empty. "
            "Reset/recreate the target schema before running this merge again."
        )

    # id_maps[table_name][source_label][old_id] = new_id
    id_maps = {table: {} for table in TABLES}

    for source in SOURCES:
        label = source["label"]
        print(f"\n=== Merging source: {label} ===")
        src_conn = connect(source)
        src_cur = src_conn.cursor(dictionary=True)

        for table in MERGE_ORDER:
            # Some teammate databases use slightly older schema versions.
            src_cur.execute(
                """
                SELECT COUNT(*) AS cnt
                FROM information_schema.tables
                WHERE table_schema = %s AND table_name = %s
                """,
                (source["database"], table),
            )
            if src_cur.fetchone()["cnt"] == 0:
                print(f"  {table}: table not present in {label} -- skipped")
                continue

            target_cur.execute(
                """
                SELECT COUNT(*) AS cnt
                FROM information_schema.tables
                WHERE table_schema = %s AND table_name = %s
                """,
                (TARGET["database"], table),
            )
            if target_cur.fetchone()["cnt"] == 0:
                print(f"  {table}: table not present in target -- skipped")
                continue

            config = TABLES[table]
            pk = config["pk"]
            fk_columns = config["fk_columns"]
            shares_pk_with = config.get("shares_pk_with")
            id_maps[table].setdefault(label, {})

            target_columns = get_columns(target_cur, TARGET["database"], table)

            src_cur.execute(f"SELECT * FROM `{table}`")
            rows = src_cur.fetchall()
            print(f"  {table}: {len(rows)} row(s) from {label}")

            warned_dropped = set()

            for row in rows:
                new_row = dict(row)
                old_pk_value = new_row.get(pk) if pk else None

                # Rewrite FK columns from this source's old IDs to target IDs.
                for fk_col, ref_table in fk_columns.items():
                    old_val = new_row.get(fk_col)
                    if old_val is None:
                        continue

                    ref_map = id_maps[ref_table].get(label, {})
                    if old_val in ref_map:
                        new_row[fk_col] = ref_map[old_val]
                    else:
                        print(
                            f"    WARNING: {table}.{fk_col}={old_val} "
                            f"has no mapping in {ref_table} for {label}; "
                            "row may be skipped by a target FK constraint."
                        )

                # Remove columns that do not exist in target and let target
                # defaults handle NULLs for NOT NULL DEFAULT columns.
                new_row, dropped = prepare_row_for_target(
                    table, new_row, target_columns
                )
                for col in dropped:
                    key = (table, col, label)
                    if key not in warned_dropped:
                        print(
                            f"    NOTE: {table}.{col} exists in {label} "
                            "but not in target -- column ignored."
                        )
                        warned_dropped.add(key)

                # Composite-key junction table.
                if pk is None:
                    cols = list(new_row.keys())
                    if not cols:
                        continue
                    placeholders = ", ".join(["%s"] * len(cols))
                    col_list = ", ".join(f"`{c}`" for c in cols)
                    sql = (
                        f"INSERT IGNORE INTO `{table}` ({col_list}) "
                        f"VALUES ({placeholders})"
                    )
                    try:
                        target_cur.execute(sql, [new_row[c] for c in cols])
                    except Error as e:
                        print(f"    ERROR inserting into {table}: {e}")
                    continue

                # Joined-table inheritance: student/landlord/admin PK must
                # equal the mapped users.userID.
                if shares_pk_with:
                    ref_table, _ref_col = shares_pk_with
                    ref_map = id_maps[ref_table].get(label, {})
                    if old_pk_value not in ref_map:
                        print(
                            f"    WARNING: {table} old {pk}={old_pk_value} "
                            f"has no mapped {ref_table} row -- skipped."
                        )
                        continue

                    new_pk = ref_map[old_pk_value]
                    new_row[pk] = new_pk
                    cols = list(new_row.keys())
                    placeholders = ", ".join(["%s"] * len(cols))
                    col_list = ", ".join(f"`{c}`" for c in cols)
                    sql = (
                        f"INSERT IGNORE INTO `{table}` ({col_list}) "
                        f"VALUES ({placeholders})"
                    )
                    try:
                        target_cur.execute(sql, [new_row[c] for c in cols])
                        id_maps[table][label][old_pk_value] = new_pk
                    except Error as e:
                        print(f"    ERROR inserting into {table}: {e}")
                    continue

                # For normal surrogate-PK tables, do not insert the old PK.
                new_row.pop(pk, None)

                # First map known shared reference rows by a stable natural key.
                existing_id = find_existing_by_natural_key(
                    target_cur, table, new_row, pk
                )

                # Then collapse an exact duplicate row if another source already
                # contributed the same baseline record.
                if existing_id is None:
                    existing_id = find_existing_exact(
                        target_cur, table, new_row, pk
                    )

                if existing_id is not None:
                    if old_pk_value is not None:
                        id_maps[table][label][old_pk_value] = existing_id
                    continue

                cols = list(new_row.keys())
                if not cols:
                    print(f"    WARNING: {table} row had no insertable columns -- skipped")
                    continue

                placeholders = ", ".join(["%s"] * len(cols))
                col_list = ", ".join(f"`{c}`" for c in cols)
                sql = (
                    f"INSERT INTO `{table}` ({col_list}) "
                    f"VALUES ({placeholders})"
                )

                try:
                    target_cur.execute(sql, [new_row[c] for c in cols])
                    new_id = target_cur.lastrowid
                    if old_pk_value is not None:
                        id_maps[table][label][old_pk_value] = new_id
                except Error as e:
                    print(
                        f"    ERROR inserting into {table} "
                        f"(old {pk}={old_pk_value}): {e}"
                    )

        src_cur.close()
        src_conn.close()

    target_conn.commit()
    target_cur.close()
    target_conn.close()
    print("\n=== Merge complete. Review warnings/errors before trusting the result. ===")


if __name__ == "__main__":
    merge()


# ---------------------------------------------------------------------------
# NOTES -- read before running
# ---------------------------------------------------------------------------
#
# 1. Run only against a FRESH/EMPTY target database. This version deliberately
#    refuses to run if the target already contains merged rows.
#
# 2. Duplicate baseline/reference rows are handled safely:
#      - users are matched by email
#      - amenities are matched by name
#      - adminnotificationreads are matched by eventKey
#    Exact duplicate rows in other auto-ID tables are also reused rather than
#    inserted a second time.
#
# 3. Source columns that do not exist in the target schema are ignored with a
#    NOTE in the console. This handles teammate schema drift such as older/newer
#    review or notification columns.
#
# 4. If a source provides NULL for a target NOT NULL column that has a default,
#    the column is omitted from the INSERT so MySQL applies the target default.
#    This fixes cases like property.capacity -> DEFAULT 1.
#
# 5. `property_panorama`, `panorama_hotspot`, `notifications`, `report`, and
#    `reports` still depend on the FK mappings above being semantically correct.
#    Review warnings and spot-check relationships after the merge.
#
# 6. Original source databases are only read. The script writes only to TARGET.
