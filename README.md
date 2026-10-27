# End-to-End CI/CD Pipeline on AWS

A portfolio project demonstrating a Java application delivery pipeline using GitHub, Jenkins, Maven, SonarQube, Docker, Amazon ECR, and Amazon EKS.

## Pipeline

1. A developer pushes code to GitHub.
2. A GitHub webhook triggers Jenkins.
3. Jenkins compiles and tests the Java application with Maven.
4. SonarQube analyzes the code and the pipeline waits for the configured quality gate.
5. Jenkins builds a Docker image and pushes a versioned image to Amazon ECR.
6. Jenkins updates the Kubernetes Deployment on Amazon EKS and waits for rollout completion.
7. If the deployment rollout fails, Jenkins attempts `kubectl rollout undo`.

> This repository is the application and pipeline configuration. It is not proof of a deployed AWS environment. Configure your own AWS resources and Jenkins credentials, then capture evidence of successful runs before claiming the full pipeline is operational.

## Repository structure

```text
.
├── src/
│   ├── main/java/com/example/demo/DemoApplication.java
│   └── test/java/com/example/demo/DemoApplicationTest.java
├── k8s/
│   ├── deployment.yaml
│   └── service.yaml
├── Dockerfile
├── Jenkinsfile
├── pom.xml
├── sonar-project.properties
└── .gitignore
```

## Prerequisites

- Git and a GitHub account
- Java 17 and Maven for local testing
- Docker
- AWS account and AWS CLI
- An ECR repository
- An EKS cluster with a working `kubectl` context
- Jenkins with Pipeline, Git, Maven/JDK, SonarQube Scanner, and Credentials plugins
- A configured SonarQube server and a Jenkins SonarQube installation named `SonarQube`
- Jenkins agent with AWS CLI, Docker CLI/daemon access, `kubectl`, and `jq`

## 1. Test locally

```bash
mvn test
mvn package
docker build -t aws-cicd-demo:local .
docker run --rm -p 8080:8080 aws-cicd-demo:local
```

Open `http://localhost:8080/` or check `http://localhost:8080/health`.

## 2. Create AWS resources

Create an ECR repository and an EKS cluster in your chosen AWS region. Ensure the Jenkins agent can reach the cluster API and that its AWS IAM role has only the permissions needed for ECR access and EKS authentication. Kubernetes RBAC must also authorize the Jenkins identity to update the target namespace.

AWS resources can incur charges. In particular, EKS control planes, worker compute, load balancers, NAT gateways, and storage can cost money. Review current AWS pricing and clean up resources when finished.

Set these Jenkins environment variables (for example in the pipeline job configuration):

- `AWS_REGION` — e.g. `ap-south-1`
- `AWS_ACCOUNT_ID` — your 12-digit AWS account ID
- `ECR_REPOSITORY` — e.g. `aws-cicd-demo`
- `EKS_CLUSTER_NAME` — your EKS cluster name
- `K8S_NAMESPACE` — e.g. `default`

Do not commit AWS access keys, SonarQube tokens, kubeconfig files, or other secrets to Git. Prefer an IAM role for the Jenkins agent and short-lived credentials.

## 3. Configure Jenkins

1. Install the plugins listed above.
2. Configure the SonarQube server under **Manage Jenkins → System** with the name `SonarQube`.
3. Create a Pipeline job pointed at this GitHub repository and select **Pipeline script from SCM** (Git).
4. Configure the job's GitHub webhook trigger, and add a webhook to the repository pointing to your Jenkins endpoint, using the endpoint and security settings appropriate to your setup.
5. Set the required environment variables in the job configuration.
6. Make sure the Jenkins agent can run Docker, AWS CLI, `kubectl`, and `jq`, and that it can authenticate to EKS.
7. Configure the SonarQube project/permissions and quality gate.

The `Jenkinsfile` assumes a Jenkins agent label named `linux-docker` that has the required tools installed. Change the label to match your agent.

## 4. Verify

- A commit triggers a Jenkins build.
- Maven tests pass.
- SonarQube analysis completes and the quality gate passes.
- A versioned image appears in ECR.
- The Kubernetes Deployment rolls out successfully.
- The application responds through the service endpoint appropriate to your cluster configuration.

The Kubernetes Service is `ClusterIP` by default to avoid exposing the demo publicly. To access it for testing, use `kubectl port-forward` or deliberately configure an ingress/load balancer with appropriate access controls.

## Cleanup

Delete the demo Kubernetes resources, ECR images/repository, EKS cluster, and any supporting infrastructure when you no longer need them. Verify all resources in the AWS console to avoid ongoing charges.

## Portfolio evidence checklist

- [ ] GitHub repository with a clear README
- [ ] Jenkins successful build screenshot
- [ ] SonarQube quality gate screenshot
- [ ] ECR image screenshot showing a build tag
- [ ] `kubectl rollout status deployment/aws-cicd-demo` output
- [ ] Application health endpoint response
- [ ] Architecture diagram and a short explanation of the rollback behavior
