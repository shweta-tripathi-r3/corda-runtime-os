# Running the Scaffold demo

## Demo CPIs

The CPIs are checked in at testing/scaffold-demo-ui/cpbs

To rebuild them, follow the instructions at 
https://github.com/corda/corda-runtime-os/wiki/Running-a-complete-flow-using-a-basic-Docker-compose-cluster

The group policy JSON is checked in here - change the group to `connect4` or `tic-tac-toe` accordingly - it is 
important that 2 different CPIs have different group ids!

## Building and starting the cluster

The `values.yaml` and `dev.yaml` checked in here allow running up a cluster from local builds (from this branch).

- First start minikube and set the docker environment correctly (not required when using Docker desktop as k8s context).
    ```
    minikube start --memory 8000 --cpus 6
    minikube docker-env --shell=powershell | Invoke-Expression
  ```
- Building and publishing OSGi images using:
  ```
  ./gradlew :applications:workers:release:db-worker:publishOSGiImage :applications:workers:release:rpc-worker:publishOSGiImage :applications:workers:release:flow-worker:publishOSGiImage :applications:workers:release:crypto-worker:publishOSGiImage
  ```

- Deploy the pre-reqs
    ```
    helm install prereqs -n corda `
    oci://corda-os-docker.software.r3.com/helm-charts/corda-prereqs `
    --set kafka.replicaCount=1,kafka.zookeeper.replicaCount=1 `
    --render-subchart-notes `
    --wait
  ```
- Deploy corda:
 ```
 helm upgrade --install corda -n corda `
   oci://corda-os-docker.software.r3.com/helm-charts/corda `
   --version ^0.1.0-beta `
   --values values.yaml `
   --values dev.yaml `
   --wait
 ```

Detailed instructions on running up the k8s cluster can be found at:
https://r3-cev.atlassian.net/wiki/spaces/CB/pages/3912957998/Local+Development+with+Kubernetes

I am using k9s for controlling the cluster:
\<shift-f\> on the RPC worker to port-forward port 8888

For debugging or DB access, you need to forward the ports on the required pods

\<l\> for logs.

\<:deploy\> and then \<s\> to scale up and down workers 

## GUI

go to `testing/scaffold-demo-ui` and run `npm install` and `npm start`