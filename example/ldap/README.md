# LDAP Test Server

A ready-to-use OpenLDAP test environment with sample users, groups, and a web UI.

## Requirements

- Docker & Docker Compose

## Setup

Place both files in the same directory:

```
your-folder/
├── docker-compose.yml
└── ldif/
    └── testdata.ldif
```

Start the stack:

```bash
docker compose up -d
```

> **Note:** The LDIF data is only loaded on the very first start (empty volume).  
> If you started the container before adding the `ldif/` folder, reset with:
> ```bash
> docker compose down -v && docker compose up -d
> ```

---

## Services

| Service      | URL / Port              | Description             |
|--------------|-------------------------|-------------------------|
| OpenLDAP     | `localhost:389` (LDAP)  | Main directory server   |
| OpenLDAP     | `localhost:636` (LDAPS) | TLS disabled by default |
| phpLDAPadmin | `http://localhost:8080` | Web UI                  |

---

## Credentials

| Role     | DN                              | Password   |
|----------|---------------------------------|------------|
| Admin    | `cn=admin,dc=example,dc=com`    | `admin`    |
| Readonly | `cn=readonly,dc=example,dc=com` | `readonly` |

**Base DN:** `dc=example,dc=com`

---

## Directory Structure

```
dc=example,dc=com
├── ou=users
│   ├── uid=alice
│   ├── uid=bob
│   └── uid=charlie
└── ou=groups
    ├── cn=admins
    ├── cn=developers
    └── cn=all-users
```

---

## Test Users

All users are `inetOrgPerson` + `posixAccount`.

| uid       | Password     | Department  | Title             |
|-----------|--------------|-------------|-------------------|
| `alice`   | `alice123`   | Engineering | Senior Developer  |
| `bob`     | `bob456`     | Marketing   | Marketing Manager |
| `charlie` | `charlie789` | IT          | Intern            |

### Attributes per user

| Attribute          | Example (alice)                    |
|--------------------|------------------------------------|
| `uid`              | alice                              |
| `cn`               | Alice Muster                       |
| `sn`               | Muster                             |
| `givenName`        | Alice                              |
| `displayName`      | Alice Muster                       |
| `mail`             | alice@example.com                  |
| `userPassword`     | alice123                           |
| `telephoneNumber`  | +49 531 1000                       |
| `mobile`           | +49 151 10000001                   |
| `postalAddress`    | Musterstraße 1, 38100 Braunschweig |
| `departmentNumber` | Engineering                        |
| `title`            | Senior Developer                   |
| `employeeNumber`   | EMP001                             |
| `employeeType`     | fulltime                           |
| `uidNumber`        | 1001                               |
| `gidNumber`        | 1001                               |
| `homeDirectory`    | /home/alice                        |
| `loginShell`       | /bin/bash                          |

---

## Groups

| Group DN | Members |
|---|---|
| `cn=admins,ou=groups,dc=example,dc=com` | alice |
| `cn=developers,ou=groups,dc=example,dc=com` | alice, bob |
| `cn=all-users,ou=groups,dc=example,dc=com` | alice, bob, charlie |

---

## Quick Tests

**Search all users:**
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=example,dc=com" -w admin \
  -b "ou=users,dc=example,dc=com" "(objectClass=inetOrgPerson)"
```

**Bind as a user (authentication test):**
```bash
ldapwhoami -x -H ldap://localhost:389 \
  -D "uid=alice,ou=users,dc=example,dc=com" -w alice123
```

**Search groups:**
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=example,dc=com" -w admin \
  -b "ou=groups,dc=example,dc=com" "(objectClass=groupOfNames)"
```

**Filter by department:**
```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=example,dc=com" -w admin \
  -b "ou=users,dc=example,dc=com" "(departmentNumber=Engineering)"
```

**Load LDIF manually** (if auto-import didn't work):
```bash
ldapadd -x -H ldap://localhost:389 \
  -D "cn=admin,dc=example,dc=com" -w admin \
  -f ldif/testdata.ldif
```

---

## Stop & Cleanup

```bash
# Stop containers, keep data
docker compose down

# Stop and delete all data (full reset)
docker compose down -v
```
