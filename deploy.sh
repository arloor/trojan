echo "请输入服务器地址"
read host
mvn clean package
scp target/trojan-1.0-SNAPSHOT-all.jar root@${host}:~/
ssh root@${host} '
pid=$(jps -l|grep trojan|awk '\''{print $1}'\'')
kill -9 $pid
(/usr/bin/java -XX:+UseZGC -Dio.netty.leakDetectionLevel=paranoid -XX:ZCollectionInterval=180 -XX:+ZProactive -jar trojan-1.0-SNAPSHOT-all.jar  444 1cdab55b4ce7bfba2ed990929372dcad6b339fff80aafd0001952057 /root/.acme.sh/someme.me/fullchain.cer /root/.acme.sh/someme.me/someme.me.key &)
'