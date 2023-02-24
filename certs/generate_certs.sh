keytool -genkeypair -alias rsocket -keyalg RSA -keysize 2048 -storetype PKCS12 -validity 3650 -keystore rsocket-server.p12 -storepass password

keytool -exportcert -alias rsocket -keystore rsocket-server.p12 -storepass password -file cert.pem

keytool -importcert -alias rsocket -keystore client.truststore -storepass password -file cert.pem
