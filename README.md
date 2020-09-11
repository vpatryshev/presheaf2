# presheaf2
A totally new version of presheaf.com - based on akkaHTTP and using HTTPS.
See application.conf which specifies that yes, we need the client ip logged.

Now runs as https://presheaf.com

https://us-west-2.console.aws.amazon.com/ec2/v2/home?region=us-west-2#Instances:sort=instanceState

### How I solved https problems

#### Using `certbot` aka `lets

Installing and setting up `certbot` - see [letsencrypt](https://letsencrypt.org/getting-started/)

When you follow instructions "with shell access", you create a bunch of files in letsencrypt's folder, and also, in the process, a password is created. I put the password into `password` file, and used it for testing too.

#### Testing https locally
I added a [class HttpsServer](https://github.com/vpatryshev/presheaf2/blob/master/src/main/scala/com/presheaf/http/HttpsServer.scala).

To test if it works, I ran `Server` locally, for which I needed credentials. 
As a password, I used the password from the steps above (and copied the file).

Here are the commands:
```shell script
openssl genrsa -des3 -out server.key 2048
openssl req -new -key server.key -out server.csr
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
cat server.key > server.pem
cat server.crt >> server.pem 
openssl pkcs12 -export -in server.pem -out keystore.presheaf.pkcs12
```

Launching the server in IntelliJ (or in `sbt run`), I tried [https://localhost:8714](https://localhost:8714) to make sure that stuff works, although brousers complain about self-signed certificates.

Then I deployed presheaf to the instance (`build` and `deploy.sh` scripts).

#### Configuring In The Cloud
I have a script, `update_from_certbot`, that, essentially, does this:
```shell script
export CBDIR=/etc/letsencrypt/live
export CERTDIR=$CBDIR/presheaf.com

openssl pkcs12 -export -out presheaf2/keystore.presheaf.pkcs12 -ixypicnkey $CERTDIR/privkey.pem -in $CERTDIR/cert.pem -certfile $CERTDIR/chain.pem

./update2
```

The script just creates a `pkcs12` based on certbot's credentials, and then it restarts presheaf, via `update2` script.

Got a "B" in ssl labs: https://www.ssllabs.com/ssltest/analyze.html?d=presheaf.com
