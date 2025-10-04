# Docker Build & OCI Registry Push

## OCI Registry konfiguráció (Biztonságos módszer)

### 1. Credentials kezelése (NE tedd git-re!)

A pom.xml-ben csak placeholder értékek vannak. A valós credentials-okat így add meg:

#### Módszer 1: Maven parancssorban
```bash
mvn clean compile jib:build \
  -Doci.registry.url=fra.ocir.io/mytenancy \
  -Doci.username=mytenancy/myuser \
  -Doci.password=my-auth-token
```

#### Módszer 2: Environment változókkal
```bash
export OCI_REGISTRY_URL=fra.ocir.io/mytenancy
export OCI_USERNAME=mytenancy/myuser  
export OCI_PASSWORD=my-auth-token

mvn clean compile jib:build \
  -Doci.registry.url=${OCI_REGISTRY_URL} \
  -Doci.username=${OCI_USERNAME} \
  -Doci.password=${OCI_PASSWORD}
```

#### Módszer 3: Maven settings.xml (lokális)
Hozz létre/frissítsd a `~/.m2/settings.xml` fájlt:
```xml
<settings>
  <profiles>
    <profile>
      <id>oci-registry</id>
      <properties>
        <oci.registry.url>fra.ocir.io/mytenancy</oci.registry.url>
        <oci.username>mytenancy/myuser</oci.username>
        <oci.password>my-auth-token</oci.password>
      </properties>
    </profile>
  </profiles>
</settings>
```

Majd használd:
```bash
mvn clean compile jib:build -P oci-registry
```

### 2. OCI Registry adatok

- **Registry URL**: `{region}.ocir.io/{tenancy-namespace}`
- **Username**: `{tenancy-namespace}/{oci-username}`
- **Password**: Auth Token (nem az OCI jelszó!)

### 3. Build parancsok

```bash
# Egyszerű build parancssorral
mvn jib:build -Doci.registry.url=fra.ocir.io/mytenancy -Doci.username=myuser -Doci.password=mytoken

# Environment változókkal
mvn jib:build -Doci.registry.url=${OCI_REGISTRY_URL} -Doci.username=${OCI_USERNAME} -Doci.password=${OCI_PASSWORD}

# Maven profile-lal (settings.xml-ben definiált)
mvn jib:build -P oci-registry

# Csak lokális build teszteléshez
mvn jib:dockerBuild
```

### 4. .gitignore ajánlások

Győződj meg róla, hogy ezek a fájlok ne kerüljenek git-re:
```
# Maven
.m2/settings.xml

# Environment files
.env
*.env

# IDE files
.idea/
*.iml
```

### 5. CI/CD pipeline-hoz

GitHub Actions / GitLab CI esetén használj secrets-okat:
```yaml
env:
  OCI_REGISTRY_URL: ${{ secrets.OCI_REGISTRY_URL }}
  OCI_USERNAME: ${{ secrets.OCI_USERNAME }}
  OCI_PASSWORD: ${{ secrets.OCI_PASSWORD }}

run: |
  mvn jib:build \
    -Doci.registry.url=${OCI_REGISTRY_URL} \
    -Doci.username=${OCI_USERNAME} \
    -Doci.password=${OCI_PASSWORD}
```
