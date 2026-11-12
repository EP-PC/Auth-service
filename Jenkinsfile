pipeline {
    agent any
    environment {
        SERVICE = 'auth'
        NAME = "pedrohmm1/${env.SERVICE}"
    }
    stages {
        stage('Dependencies') {
            steps {
                build job: 'account', wait: true
            }
        }
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Build & Push Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credential', usernameVariable: 'USERNAME', passwordVariable: 'TOKEN')]) {
                    sh 'echo $TOKEN | docker login -u $USERNAME --password-stdin'
                    sh "docker buildx create --use --platform=linux/arm64,linux/amd64 --node multi-platform-builder-${env.SERVICE} --name multi-platform-builder-${env.SERVICE}"
                    sh "docker buildx build --platform=linux/arm64,linux/amd64 --push --tag ${env.NAME}:latest --tag ${env.NAME}:${env.BUILD_ID} -f Dockerfile ."
                    sh "docker buildx rm --force multi-platform-builder-${env.SERVICE}"
                }
            }
        }
        stage('Deploy to EKS') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-credential',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        export AWS_DEFAULT_REGION=us-east-2
                        trap 'rm -f ./kubeconfig' EXIT
                        if ! out=$(aws eks describe-cluster --name eks-store 2>&1); then
                            if echo "$out" | grep -q ResourceNotFoundException; then
                                echo "Cluster eks-store nao esta de pe: pulando o deploy"; exit 0
                            fi
                            echo "$out"; exit 1
                        fi
                        aws eks update-kubeconfig --name eks-store --kubeconfig ./kubeconfig
                        kubectl --kubeconfig ./kubeconfig set image deployment/${SERVICE} ${SERVICE}=${NAME}:${BUILD_ID}
                        kubectl --kubeconfig ./kubeconfig rollout status deployment/${SERVICE} --timeout=300s
                    '''
                }
            }
        }
    }
}
