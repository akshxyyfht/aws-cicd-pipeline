
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        AWS_REGION      = 'ap-south-1'
        AWS_ACCOUNT_ID  = '194154437244'
        ECR_REPOSITORY  = 'aws-cicd-pipeline'
        EKS_CLUSTER     = 'aws-cicd-cluster-v1'
        NODEGROUP       = 'aws-cicd-workers-v1'
        K8S_DEPLOYMENT  = 'aws-cicd-app'
        K8S_SERVICE     = 'aws-cicd-service'
        APP_NAME        = 'aws-cicd-app'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools and AWS Access') {
            steps {
                sh '''
                    set -eu
                    command -v aws
                    command -v docker
                    command -v kubectl
                    command -v eksctl
                    command -v mvn

                    aws sts get-caller-identity
                    aws configure get region || true
                '''
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('Create ECR Repository if Missing') {
            steps {
                sh '''
                    set -eu

                    if ! aws ecr describe-repositories \
                        --repository-names "$ECR_REPOSITORY" \
                        --region "$AWS_REGION" >/dev/null 2>&1; then

                        aws ecr create-repository \
                            --repository-name "$ECR_REPOSITORY" \
                            --image-scanning-configuration scanOnPush=true \
                            --region "$AWS_REGION"
                    fi

                    aws ecr describe-repositories \
                        --repository-names "$ECR_REPOSITORY" \
                        --region "$AWS_REGION"
                '''
            }
        }

        stage('Build and Push Docker Image') {
            steps {
                sh '''
                    set -eu

                    ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
                    IMAGE_TAG="$BUILD_NUMBER"

                    aws ecr get-login-password --region "$AWS_REGION" |
                        docker login \
                            --username AWS \
                            --password-stdin \
                            "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

                    docker build -t "$ECR_REPOSITORY:$IMAGE_TAG" .

                    docker tag "$ECR_REPOSITORY:$IMAGE_TAG" \
                        "$ECR_URI:$IMAGE_TAG"

                    docker tag "$ECR_REPOSITORY:$IMAGE_TAG" \
                        "$ECR_URI:latest"

                    docker push "$ECR_URI:$IMAGE_TAG"
                    docker push "$ECR_URI:latest"

                    echo "Pushed image: $ECR_URI:$IMAGE_TAG"
                '''
            }
        }

        stage('Create EKS Cluster if Missing') {
            steps {
                sh '''
                    set -eu

                    if aws eks describe-cluster \
                        --name "$EKS_CLUSTER" \
                        --region "$AWS_REGION" >/dev/null 2>&1; then

                        echo "EKS cluster already exists."
                    else
                        echo "Creating EKS cluster and managed node group..."

                        eksctl create cluster \
                            --name "$EKS_CLUSTER" \
                            --region "$AWS_REGION" \
                            --version 1.33 \
                            --managed \
                            --nodegroup-name "$NODEGROUP" \
                            --node-type t3.small \
                            --nodes 1 \
                            --nodes-min 1 \
                            --nodes-max 2 \
                            --with-oidc
                    fi

                    aws eks wait cluster-active \
                        --name "$EKS_CLUSTER" \
                        --region "$AWS_REGION"

                    aws eks update-kubeconfig \
                        --name "$EKS_CLUSTER" \
                        --region "$AWS_REGION"

                    kubectl cluster-info
                    kubectl get nodes
                '''
            }
        }

        stage('Ensure Worker Node Group') {
            steps {
                sh '''
                    set -eu

                    if ! aws eks describe-nodegroup \
                        --cluster-name "$EKS_CLUSTER" \
                        --nodegroup-name "$NODEGROUP" \
                        --region "$AWS_REGION" >/dev/null 2>&1; then

                        eksctl create nodegroup \
                            --cluster "$EKS_CLUSTER" \
                            --region "$AWS_REGION" \
                            --name "$NODEGROUP" \
                            --managed \
                            --node-type t3.small \
                            --nodes 1 \
                            --nodes-min 1 \
                            --nodes-max 2
                    fi

                    aws eks wait nodegroup-active \
                        --cluster-name "$EKS_CLUSTER" \
                        --nodegroup-name "$NODEGROUP" \
                        --region "$AWS_REGION"

                    kubectl wait \
                        --for=condition=Ready nodes \
                        --all \
                        --timeout=10m
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    set -eu

                    ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"

                    kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $K8S_DEPLOYMENT
spec:
  replicas: 1
  selector:
    matchLabels:
      app: $APP_NAME
  template:
    metadata:
      labels:
        app: $APP_NAME
    spec:
      containers:
        - name: $APP_NAME
          image: $ECR_URI:$BUILD_NUMBER
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 20
---
apiVersion: v1
kind: Service
metadata:
  name: $K8S_SERVICE
spec:
  type: LoadBalancer
  selector:
    app: $APP_NAME
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP
EOF

                    kubectl rollout status \
                        deployment/"$K8S_DEPLOYMENT" \
                        --timeout=5m

                    kubectl get deployments
                    kubectl get pods -o wide
                    kubectl get services
                '''
            }
        }

        stage('Verify Live Application') {
            steps {
                sh '''
                    set -eu

                    echo "Waiting for the external LoadBalancer address..."

                    for attempt in $(seq 1 30); do
                        APP_HOST=$(kubectl get service "$K8S_SERVICE" \
                            -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

                        if [ -n "$APP_HOST" ]; then
                            break
                        fi

                        sleep 10
                    done

                    if [ -z "${APP_HOST:-}" ]; then
                        echo "LoadBalancer address is not ready yet."
                        kubectl describe service "$K8S_SERVICE"
                        exit 1
                    fi

                    echo "Application URL: http://$APP_HOST/"
                    echo "Health URL: http://$APP_HOST/health"

                    curl --fail --retry 5 --retry-delay 5 \
                        --retry-connrefused \
                        "http://$APP_HOST/health"
                '''
            }
        }
    }

    post {
        success {
            echo 'CI/CD SUCCESS: image pushed and application deployed.'
        }

        failure {
            echo 'CI/CD FAILED: inspect the failed stage in the Jenkins console log.'
        }

        always {
            sh 'docker logout "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com" || true'
        }
    }
}
