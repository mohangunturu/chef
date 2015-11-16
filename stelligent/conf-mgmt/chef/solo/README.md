Prepare the bundle
------------------
mvn package creates devops war file..



```bash
mkdir -p stelligent-mini-project/deploy_hooks
cd stelligent-mini-project
```

Then create a simple AppSpec as `./appspec.yml`:

```yml
version: 0.0
os: linux
files:
  - source: chef/
    destination: /etc/chef/codedeploy
  - source: target/devops.war
    destination: /var/lib/tomcat6/webapps
hooks:
  BeforeInstall:
    - location: deploy_hooks/install-chef.sh
      timeout: 1800
      runas: root
  ApplicationStart:
    - location: deploy_hooks/chef-solo.sh
      runas: root
  ValidateService:
    - location: deploy_hooks/verify_service.sh
      runas: root
```


--AWS code deply and tomcat test

```bash
#!/bin/bash

yum list installed rubygems &> /dev/null
if [ $? != 0 ]; then
    yum -y install gcc-c++ ruby-devel make autoconf automake rubygems
fi

gem list | grep -q chef
if [ $? != 0 ]; then
    gem install chef ohai
fi

# Install the tomcat cookbook
yum list installed git &> /dev/null
if [ $? != 0 ]; then
    yum install -y git
fi

cd /etc/chef/codedeploy/
if ! test -r .git; then 
    git init .; git add -A .; git commit -m "Init commit"
fi
if ! test -r ./cookbooks/tomcat; then
    /usr/local/bin/knife cookbook site install tomcat -o ./cookbooks
fi
```

Then, once our files are installed into the correct locations, our `ApplicationStart` lifecycle hook
actually initiates the chef-solo run::

```bash
#!/bin/bash
/usr/local/bin/chef-solo -c /etc/chef/codedeploy/chef/solo.rb
```

Finally, the `ValidateService` hook checks to see whether or not our app is responding as expected:

```bash
#!/bin/bash

result=$(curl -s http://localhost/devops/)

if [[ "$result" =~ "Automation for the People" ]]; then
    exit 0
else
    exit 1
fi
```

Our chef configuration in this case is simply to set a couple of default tomcat options:

```ruby
node.default["tomcat"]["user"] = "root"
node.default["tomcat"]["port"] = 80
```

And the node.json and solo.rb configurations are similarly straightforward, just running the tomcat
default recipe and mini for web page...

node.json:

```javascript
{
  "run_list": [ "recipe[mini]", "recipe[tomcat]" ]
}
```

solo.rb:

```ruby
file_cache_path "/etc/chef/codedeploy/"
cookbook_path "/etc/chef/codedeploy/cookbooks"
json_attribs "/etc/chef/codedeploy/node.json"
```

The java app does nothing more than respond with 'Automation for the People' at the root of the app.

Set Up the AWS CodeDeploy Application
------------------------------
```sh
aws deploy create-application --application-name stelligent-mini-project
```



```sh
aws deploy create-deployment-group \
    --application-name stelligent-mini-project \
    --deployment-group-name Stelligent_DeploymentGroup \
    --deployment-config-name CodeDeployDefault.AllAtOnce \
    --ec2-tag-filters Key=Name,Value=CodeDeployDeployment,Type=KEY_AND_VALUE \
    --service-role-arn CodeDeployTrustRoleArn( Use role AWS cloud formation stack provided)
```


Push and Deploy the Application
-------------------------------


```sh
aws deploy push \
    --application-name stelligent-mini-project \
    --s3-location s3://mini/stelligent-mini.zip \
    --ignore-hidden-files
```

And now we're ready for a deployment:

```sh
aws deploy create-deployment \
    --application-name stelligent-mini-project \
    --deployment-config-name CodeDeployDefault.AllAtOnce \
    --deployment-group-name Stelligent_DeploymentGroup \
    --s3-location bucket=mini,key=stelligent-mini.zip,bundleType=zip
```
