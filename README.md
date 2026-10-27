# 🚀 End-to-End CI/CD Pipeline on AWS using Jenkins, Docker, SonarQube & Amazon EKS

![AWS](https://img.shields.io/badge/Cloud-AWS-orange?logo=amazonaws)
![Jenkins](https://img.shields.io/badge/CI%2FCD-Jenkins-red?logo=jenkins)
![Docker](https://img.shields.io/badge/Container-Docker-blue?logo=docker)
![Kubernetes](https://img.shields.io/badge/Orchestration-Kubernetes-326CE5?logo=kubernetes)
![Java](https://img.shields.io/badge/Language-Java-ED8B00?logo=openjdk)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)
![SonarQube](https://img.shields.io/badge/Code%20Quality-SonarQube-4E9BCD?logo=sonarqube)

## 📌 Project Overview

This project demonstrates an end-to-end Continuous Integration and Continuous Deployment (CI/CD) pipeline for a Java Spring Boot application using AWS and DevOps tools.

The pipeline automates source code checkout, application building, testing, static code analysis, Docker image creation, publishing container images to Amazon Elastic Container Registry (ECR), and deployment to Amazon Elastic Kubernetes Service (EKS).

Jenkins orchestrates the pipeline, while SonarQube enforces a Quality Gate before the application proceeds to containerization and deployment.

The application is deployed to Kubernetes and exposed through an AWS LoadBalancer Service, making its health endpoint accessible over HTTP.

## 🎯 Project Objectives

- Automate application build and testing with Maven.
- Integrate SonarQube for static code analysis and Quality Gate validation.
- Containerize the application using Docker.
- Store versioned container images in Amazon ECR.
- Deploy the application to Amazon EKS using Kubernetes.
- Automate deployments through Jenkins.
- Verify application availability using Kubernetes rollout status and an HTTP health endpoint.

## 🏗️ Architecture

```text
                 Developer
                     |
                     v
              GitHub Repository
                     |
                     v
                  Jenkins
                     |
             +-------+--------+
             |                |
             v                v
       Maven Build & Test   SonarQube
             |             Code Analysis
             |                |
             +------- Quality Gate
                     |
                     v
               Docker Build
                     |
                     v
              Amazon ECR
          Versioned Docker Images
                     |
                     v
             Amazon EKS Cluster
                     |
                     v
          Kubernetes Deployment
                     |
                     v
              Kubernetes Pod
                     |
                     v
           Kubernetes LoadBalancer
                     |
                     v
             Spring Boot App
                     |
                     v
                 /health
```

### Pipeline Workflow

1. **Source Checkout:** Jenkins retrieves application source code and the Jenkinsfile from GitHub.
2. **Build and Test:** Maven compiles the application and runs automated tests using `mvn clean verify`.
3. **Code Quality Analysis:** SonarQube analyzes the source code.
4. **Quality Gate:** Jenkins waits for the SonarQube Quality Gate and stops the pipeline if the gate does not pass.
5. **Docker Image Build:** Jenkins builds a container image for the application.
6. **Amazon ECR Push:** The pipeline authenticates with Amazon ECR and pushes versioned and `latest` image tags.
7. **EKS Deployment:** Jenkins updates the Kubernetes Deployment to use the image corresponding to the current Jenkins build number.
8. **Rollout Verification:** Jenkins waits for the Kubernetes rollout to complete successfully.
9. **Application Validation:** The deployed application is tested through its HTTP health endpoint.

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| AWS EC2 | Hosts Jenkins and SonarQube for the lab |
| AWS IAM | Provides AWS permissions to the Jenkins EC2 instance |
| AWS ECR | Stores Docker images |
| AWS EKS | Managed Kubernetes control plane |
| Kubernetes | Manages application deployment, pods and services |
| Jenkins | Automates CI/CD stages |
| SonarQube | Static code analysis and Quality Gate |
| Docker | Builds application container images |
| Java | Application programming language |
| Spring Boot | Application framework |
| Maven | Build and test automation |
| Git and GitHub | Source control and repository hosting |
| AWS LoadBalancer Service | Exposes the application externally |

## 📂 Repository Structure

```text
aws-cicd-pipeline/
├── src/
│   ├── main/
│   └── test/
├── k8s/
│   ├── deployment.yaml
│   └── service.yaml
├── Dockerfile
├── Jenkinsfile
├── pom.xml
├── sonar-project.properties
└── README.md
```

## ⚙️ AWS Infrastructure

The project was implemented in the AWS Mumbai Region.

| Resource | Configuration |
|---|---|
| AWS Region | `ap-south-1` |
| ECR Repository | `aws-cicd-pipeline` |
| EKS Cluster | `aws-cicd-cluster` |
| EKS Node Group | `aws-cicd-workers` |
| Kubernetes Deployment | `aws-cicd-app` |
| Kubernetes Service | `aws-cicd-service` |
| Application Container Port | `8080` |
| Service Port | `80` |

> **Note:** Resource names and configuration describe the lab environment. The EC2 instance, cluster, node group, load balancer, and related resources may be stopped, deleted, or recreated after the project is completed.

## 🔄 Jenkins Pipeline Stages

The Jenkins pipeline is defined in the `Jenkinsfile`.

### 1. Checkout

Checks out the source code from GitHub.

### 2. Build and Test

```bash
mvn -B clean verify
```

Builds the application and executes the configured Maven tests.

### 3. SonarQube Analysis

Runs SonarQube analysis using the configured Jenkins SonarQube server and authentication token.

### 4. Quality Gate

The pipeline waits for the SonarQube Quality Gate. If the status is not `OK`, the pipeline fails before image creation.

### 5. Build Docker Image

Creates versioned and `latest` Docker image tags.

```text
194154437244.dkr.ecr.ap-south-1.amazonaws.com/aws-cicd-pipeline:<BUILD_NUMBER>
```

### 6. Push to Amazon ECR

Authenticates with ECR and pushes the generated image tags.

### 7. Deploy to Amazon EKS

Updates the Kubernetes Deployment with the versioned image:

```bash
kubectl set image \
  deployment/aws-cicd-app \
  aws-cicd-app=<ECR_IMAGE_URI>:<BUILD_NUMBER>
```

Then verifies the rollout:

```bash
kubectl rollout status deployment/aws-cicd-app --timeout=180s
```

This makes each deployment traceable to its Jenkins build number.

## ☸️ Kubernetes Deployment

The application runs inside a Kubernetes Deployment with one replica in the lab configuration.

The Kubernetes Service uses the `LoadBalancer` type to expose the application through an AWS load balancer.

### Verify the Deployment

```bash
kubectl get deployments
kubectl get pods
kubectl get services
```

### Check the Running Image

```bash
kubectl get deployment aws-cicd-app \
  -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```

### Verify Application Health

```bash
curl -i http://<LOAD_BALANCER_DNS>/health
```

Replace `<LOAD_BALANCER_DNS>` with the external DNS name shown by:

```bash
kubectl get service aws-cicd-service
```

A successful health check should return HTTP `200` and the application's expected health response.

## 🔐 Security Practices

- Use an EC2 IAM role for Jenkins to access AWS services rather than hardcoding AWS access keys in the pipeline.
- Store SonarQube authentication credentials in Jenkins credentials/configuration.
- Keep security group access restricted to the required sources and ports.
- Avoid committing secrets, access tokens, passwords, or private keys to GitHub.
- Use least-privilege IAM permissions in a production environment.
- Review and remove temporary lab permissions after testing.

## 🧪 Validation and Results

The project was tested with a successful Jenkins pipeline and a working EKS deployment.

| Validation | Result |
|---|---|
| Maven build and tests | Passed |
| SonarQube Quality Gate | Passed |
| Docker image build | Passed |
| Image push to Amazon ECR | Passed |
| EKS deployment | Successful |
| Kubernetes pod status | `Running` |
| Ready replicas | `1/1` |
| Kubernetes rollout | Successful |
| Application health endpoint | HTTP `200` observed |

The Jenkins build used for the final deployment was **Build #13**. The Kubernetes Deployment successfully rolled out the versioned ECR image tagged `13`.

## 📚 Key Learnings

Through this project, I gained practical experience with:

- Designing and implementing a CI/CD pipeline using Jenkins.
- Integrating Maven and SonarQube into automated build workflows.
- Applying Quality Gate checks before deployment.
- Building and publishing Docker images to Amazon ECR.
- Configuring IAM-based AWS access for Jenkins.
- Creating and managing an Amazon EKS cluster and worker node group.
- Deploying applications with Kubernetes Deployments and Services.
- Exposing a Kubernetes application using an AWS load balancer.
- Automating Kubernetes image updates and verifying deployment rollouts.
- Troubleshooting cloud infrastructure, container and deployment issues.

## 🚀 Future Improvements

- Configure GitHub webhooks for automatic builds on every push.
- Add automated rollback when a deployment fails.
- Introduce separate development and production environments.
- Add HTTPS with a suitable certificate and domain name.
- Implement monitoring and alerting with Prometheus and Grafana.
- Replace broad temporary IAM permissions with least-privilege policies.
- Use infrastructure as code to provision the AWS environment.
- Add deployment notifications and pipeline reports.

## 👨‍💻 Author

**Akshaya Kumar M**

Cloud & DevOps | AWS | Jenkins | Docker | Kubernetes | CI/CD

- GitHub: [@akshxyyfht](https://github.com/akshxyyfht)
- Project Repository: [aws-cicd-pipeline](https://github.com/akshxyyfht/aws-cicd-pipeline)

---

⭐ If you find this project useful, feel free to explore the repository and review the pipeline implementation.
