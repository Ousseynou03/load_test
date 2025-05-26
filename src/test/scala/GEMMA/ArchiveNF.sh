endpoint_lmb='https://gemma-perf.galerieslafayette.store/ws/rovercash/nf/archive'

# Traces
# ------
exec 2> gemma_runPostNF.log

# Calcul du paramètre timestamp à partir de la date courante
# ----------------------------------------------------------
timestamp=$(date +"%s")

# Calcul du hash MD5 du paramètre body
# -------------------------------------
body=''

if [ -z "$body" ]
then
      body_md5=''
else
      body_md5=$(echo -n $body | md5sum | cut -f1 -d' ')
fi

# User+password
# ------------
user='edi17406689565'
password_md5='$2y$10$fhnNan2dzUC0KstCLk0DB.pcmiL.cGm8IMdkueDubmSIl/DFk0OK2'

request=$(echo POST:/rovercash/nf/archive?body=$body_md5\&timestamp=$timestamp\&user=$user)

# Calcul du hash MD5 de la requête HTTP
request_hmac=$(echo -n $request | openssl dgst -sha256 -hmac $password_md5 | sed 's/^.* //')

# Calcul du paramètre signature qui correspond au scellement applicatif
signature=$(echo -n  $request_hmac | base64 -w 0)

# Construction de la requêtte d'appel finale du WS LMB à monitorer
endpoint_ws=$(echo $endpoint_lmb?body=\&timestamp=$timestamp\&user=$user\&signature=$signature)

# Lancement du CURL
# -----------------
echo curl -X POST "$endpoint_ws" -H 'Content-Type: multipart/form-data;' -F id_terminal=10001 -F 'filedata=@/apps/lmb/tmp/PACK_NF/ARCHIVE_001_20250505000000.zip' -o gemma_runPostNF.out --stderr gemma_runPostNF.err > ./trace.log 2>&1

curl    -X POST "$endpoint_ws" \
        -H 'Content-Type: multipart/form-data;' \
        -F id_terminal=10001 \
        -F 'filedata=@/apps/lmb/tmp/PACK_NF/ARCHIVE_001_20250505000000.zip' \
        -o gemma_runPostNF.out --stderr gemma_runPostNF.err
