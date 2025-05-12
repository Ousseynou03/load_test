#!/bin/bash

if [ $# -eq 4 ]
  then
        user=$1
        passwordmd5=$2
        timestamp=$3
        body=$4
fi

if [ $# -eq 3 ]
  then
        user=$1
        passwordmd5=$2
        timestamp=$3
	body=''
fi

webservice=rovercash/nf/archive

if [ -z "$body" ]
then
      bodymd5=''
else
      bodymd5=$(echo -n $body | md5sum | cut -f1 -d' ')
fi

request=$(echo POST:/$webservice?body=$bodymd5\&timestamp=$timestamp\&user=$user) 
requesthmac=$(echo -n $request |openssl dgst -sha256 -hmac $passwordmd5 |sed 's/^.* //')
signature=$(echo -n $requesthmac |base64 -w 0 )
retour=$(echo -n body=$bodymd5\&timestamp=$timestamp\&user=$user\&signature=$signature)

echo $retour
