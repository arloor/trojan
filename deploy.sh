mvn clean package
scp target/trojan-1.0-SNAPSHOT-all.jar root@someme.me:~/
ssh root@someme.me '
pid=$(jps -l|grep trojan|awk '\''{print $1}'\'')
kill -9 $pid
(/usr/bin/java -XX:+UseZGC -XX:ZCollectionInterval=180 -XX:+ZProactive -jar trojan-1.0-SNAPSHOT-all.jar  444 1cdab55b4ce7bfba2ed990929372dcad6b339fff80aafd0001952057 /root/.acme.sh/someme.me/fullchain.cer /root/.acme.sh/someme.me/someme.me.key &)
'