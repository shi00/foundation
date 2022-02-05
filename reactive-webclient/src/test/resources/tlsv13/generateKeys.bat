del *.p12 *.cer

keytool -genkey -v -alias mockwebserver -keyalg RSA -keystore mockwebserver.p12 -storetype PKCS12 -validity 36500 -keysize 2048 -storepass password -dname "CN=localhost,OU=DEV,O=Silong,L=Shenzhen,ST=Guangdong,C=CN"

keytool -genkey -v -alias client -keyalg RSA -keystore client.p12 -storetype PKCS12 -validity 36500 -keysize 2048 -storepass password -dname "CN=localhost,OU=DEV,O=Silong,L=Shenzhen,ST=Guangdong,C=CN"

keytool -export -alias client -keystore client.p12 -storetype PKCS12 -storepass password -rfc -file client.cer

keytool -export -alias mockwebserver -keystore mockwebserver.p12 -storetype PKCS12 -storepass password -rfc -file mockwebserver.cer

keytool -import -alias client -keystore mockwebserver-trust.p12 -storepass password -file client.cer

keytool -import -alias mockwebserver -keystore client-trust.p12 -storepass password -file mockwebserver.cer

keytool -list -v -keystore mockwebserver-trust.p12 -storepass password
keytool -list -v -keystore client-trust.p12 -storepass password
keytool -list -v -keystore mockwebserver.p12 -storepass password
keytool -list -v -keystore client.p12 -storepass password