REM 使用RSA算法生成双向认证证书

::删除当前目录下已经生成的证书
del *.p12 *.cer

::指定生成证书的格式，文件名，主机名
set STORE_TYPE=PKCS12
set PASSWORD=password
set HOST_NAME=localhost
set SERVER=mockwebserver
set CLIENT=client
set VALIDITY=36500
set KEYSIZE=2048

::生成服务端证书
keytool -genkey -v -alias %SERVER% -keyalg RSA -keystore %SERVER%.p12 -storetype %STORE_TYPE% -validity %VALIDITY% -keysize %KEYSIZE% -storepass %PASSWORD% -dname "CN=%HOST_NAME%,OU=DEV,O=Silong,L=Shenzhen,ST=Guangdong,C=CN"

keytool -genkey -v -alias %CLIENT% -keyalg RSA -keystore %CLIENT%.p12 -storetype %STORE_TYPE% -validity %VALIDITY% -keysize %KEYSIZE% -storepass %PASSWORD% -dname "CN=%HOST_NAME%,OU=DEV,O=Silong,L=Shenzhen,ST=Guangdong,C=CN"

keytool -export -alias %CLIENT% -keystore %CLIENT%.p12 -storetype %STORE_TYPE% -storepass %PASSWORD% -rfc -file %CLIENT%.cer

keytool -export -alias %SERVER% -keystore %SERVER%.p12 -storetype %STORE_TYPE% -storepass %PASSWORD% -rfc -file %SERVER%.cer

keytool -import -alias %CLIENT% -keystore %SERVER%-trust.p12 -storepass %PASSWORD% -file %CLIENT%.cer -noprompt

keytool -import -alias %SERVER% -keystore %CLIENT%-trust.p12 -storepass %PASSWORD% -file %SERVER%.cer -noprompt

keytool -list -v -keystore %SERVER%-trust.p12 -storepass %PASSWORD%
keytool -list -v -keystore %CLIENT%-trust.p12 -storepass %PASSWORD%
keytool -list -v -keystore %SERVER%.p12 -storepass %PASSWORD%
keytool -list -v -keystore %CLIENT%.p12 -storepass %PASSWORD%