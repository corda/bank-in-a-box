@Library('existing-build-control')
import static com.r3.build.BuildControl.killAllExistingBuildsForJob

killAllExistingBuildsForJob(env.JOB_NAME, env.BUILD_NUMBER.toInteger())

pipeline {
    agent {
        dockerfile {
            filename '.ci/Dockerfile'
        }
    }
    options { timestamps() }

    environment {
        EXECUTOR_NUMBER = "${env.EXECUTOR_NUMBER}"
    }

    stages {
        stage('Unit Tests') {
            steps {
                timeout(30) {
                    sh "./gradlew clean test --info --info --stacktrace --no-daemon"
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh "./gradlew integrationTest --no-daemon"
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: false, testResults: '**/build/test-results/**/*.xml'
        }
        cleanup {
            deleteDir() /* clean up our workspace */
        }
    }
}
