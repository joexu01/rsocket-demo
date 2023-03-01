#!/bin/bash
openssl req -newkey rsa:2048 -nodes -x509 -days 365 -out ca.pem -keyout ca.key -addext 'subjectAltName = DNS:localhost' -subj "/C=CN/ST=Sichuan/L=Mianyang/O=CAEP/OU=ICA/CN=localhost/"
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr -addext 'subjectAltName = DNS:localhost' -subj "/C=CN/ST=Sichuan/L=Mianyang/O=CAEP/OU=ICA/CN=localhost/"
openssl x509 -req -extfile <(printf "subjectAltName=DNS:localhost") -in server.csr -CA ca.pem -CAkey ca.key -CAcreateserial -out server.pem -days 365 -sha256

keytool -importcert -keystore client.truststore -file server.pem