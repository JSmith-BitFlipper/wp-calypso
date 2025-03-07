import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.githubConnection
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.schedule
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

	mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2020.2"

project {

	vcsRoot(WpCalypso)
	subProject(WpDesktop)
	subProject(WPComPlugins)
	buildType(RunAllUnitTests)
	buildType(BuildBaseImages)
	buildType(CheckCodeStyle)
	buildType(CheckCodeStyleBranch)
	buildType(BuildDockerImage)
	buildType(RunCalypsoE2eDesktopTests)

	params {
		param("env.NODE_OPTIONS", "--max-old-space-size=32000")
		text("E2E_WORKERS", "16", label = "Magellan parallel workers", description = "Number of parallel workers in Magellan (e2e tests)", allowEmpty = true)
		text("env.JEST_MAX_WORKERS", "16", label = "Jest max workers", description = "How many tests run in parallel", allowEmpty = true)
		password("matticbot_oauth_token", "credentialsJSON:34cb38a5-9124-41c4-8497-74ed6289d751", display = ParameterDisplay.HIDDEN)
		param("teamcity.git.fetchAllHeads", "true")
		text("env.CHILD_CONCURRENCY", "15", label = "Yarn child concurrency", description = "How many packages yarn builds in parallel", allowEmpty = true)
		text("docker_image", "registry.a8c.com/calypso/base:latest", label = "Docker image", description = "Default Docker image used to run builds", allowEmpty = true)
		text("docker_image_e2e", "registry.a8c.com/calypso/ci-e2e:latest", label = "Docker e2e image", description = "Docker image used to run e2e tests", allowEmpty = true)
		text("calypso.run_full_eslint", "false", label = "Run full eslint", description = "True will lint all files, empty/false will lint only changed files", allowEmpty = true)
		text("env.DOCKER_BUILDKIT", "1", label = "Enable Docker BuildKit", description = "Enables BuildKit (faster image generation). Values 0 or 1", allowEmpty = true)
		password("CONFIG_E2E_ENCRYPTION_KEY", "credentialsJSON:16d15e36-f0f2-4182-8477-8d8072d0b5ec", display = ParameterDisplay.HIDDEN)
		text("env.SKIP_TSC", "true", label = "Skip TS type generation", description = "Skips running `tsc` on yarn install", allowEmpty = true)
	}

	features {
		dockerRegistry {
			id = "PROJECT_EXT_15"
			name = "Docker Registry"
			url = "registry.a8c.com"
		}
		githubConnection {
			id = "PROJECT_EXT_8"
			displayName = "GitHub.com"
			clientId = "abfe9b6b38deb65e68e5"
			clientSecret = "credentialsJSON:52797023-03f8-430a-b66f-2ac50fcc9608"
		}
	}
}

object BuildBaseImages : BuildType({
	name = "Build base images"
	description = "Build base docker images"

	buildNumberPattern = "%build.prefix%.%build.counter%"

	params {
		param("build.prefix", "1.0")
		param("image_tag", "latest")
	}

	vcs {
		root(WpCalypso)
		cleanCheckout = true
	}

	steps {
		dockerCommand {
			name = "Build base image"
			commandType = build {
				source = file {
					path = "Dockerfile.base"
				}
				namesAndTags = """
					registry.a8c.com/calypso/base:%image_tag%
					registry.a8c.com/calypso/base:%build.number%
				""".trimIndent()
				commandArgs = "--no-cache --target base"
			}
			param("dockerImage.platform", "linux")
		}
		dockerCommand {
			name = "Build CI Desktop image"
			commandType = build {
				source = file {
					path = "Dockerfile.base"
				}
				namesAndTags = """
					registry.a8c.com/calypso/ci-desktop:%image_tag%
					registry.a8c.com/calypso/ci-desktop:%build.number%
				""".trimIndent()
				commandArgs = "--target ci-desktop"
			}
			param("dockerImage.platform", "linux")
		}
		dockerCommand {
			name = "Build CI e2e image"
			commandType = build {
				source = file {
					path = "Dockerfile.base"
				}
				namesAndTags = """
					registry.a8c.com/calypso/ci-e2e:%image_tag%
					registry.a8c.com/calypso/ci-e2e:%build.number%
				""".trimIndent()
				commandArgs = "--target ci-e2e"
			}
			param("dockerImage.platform", "linux")
		}
		dockerCommand {
			name = "Build CI wpcom image"
			commandType = build {
				source = file {
					path = "Dockerfile.base"
				}
				namesAndTags = """
					registry.a8c.com/calypso/ci-wpcom:%image_tag%
					registry.a8c.com/calypso/ci-wpcom:%build.number%
				""".trimIndent()
				commandArgs = "--target ci-wpcom"
			}
			param("dockerImage.platform", "linux")
		}
		dockerCommand {
			name = "Push images"
			commandType = push {
				namesAndTags = """
					registry.a8c.com/calypso/base:%image_tag%
					registry.a8c.com/calypso/base:%build.number%
					registry.a8c.com/calypso/ci-desktop:%image_tag%
					registry.a8c.com/calypso/ci-desktop:%build.number%
					registry.a8c.com/calypso/ci-e2e:%image_tag%
					registry.a8c.com/calypso/ci-e2e:%build.number%
					registry.a8c.com/calypso/ci-wpcom:%image_tag%
					registry.a8c.com/calypso/ci-wpcom:%build.number%
				""".trimIndent()
			}
		}
	}

	triggers {
		schedule {
			schedulingPolicy = daily {
				hour = 0
			}
			branchFilter = """
				+:trunk
			""".trimIndent()
			triggerBuild = always()
			withPendingChangesOnly = false
		}
	}

	failureConditions {
		executionTimeoutMin = 30
	}

	features {
		perfmon {
		}
		dockerSupport {
			cleanupPushedImages = true
		}
	}
})

object BuildDockerImage : BuildType({
	name = "Build docker image"
	description = "Build docker image for Calypso"

	vcs {
		root(WpCalypso)
		cleanCheckout = true
	}

	steps {
		dockerCommand {
			name = "Build docker image"
			commandType = build {
				source = file {
					path = "Dockerfile"
				}
				namesAndTags = """
					registry.a8c.com/calypso/app:build-%build.number%
					registry.a8c.com/calypso/app:commit-${WpCalypso.paramRefs.buildVcsNumber}
				""".trimIndent()
				commandArgs = """
					--pull
					--label com.a8c.image-builder=teamcity
					--label com.a8c.target=calypso-live
					--label com.a8c.build-id=%teamcity.build.id%
					--build-arg workers=16
					--build-arg node_memory=32768
					--build-arg use_cache=true
				""".trimIndent().replace("\n"," ")
			}
			param("dockerImage.platform", "linux")
		}
		dockerCommand {
			commandType = push {
				namesAndTags = """
					registry.a8c.com/calypso/app:build-%build.number%
					registry.a8c.com/calypso/app:commit-${WpCalypso.paramRefs.buildVcsNumber}
				""".trimIndent()
			}
		}
	}

	failureConditions {
		executionTimeoutMin = 20
	}

	features {
		perfmon {
		}
		pullRequests {
			vcsRootExtId = "${WpCalypso.id}"
			provider = github {
				authType = token {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
				filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
			}
		}
	}
})

object RunAllUnitTests : BuildType({
	name = "Run unit tests"
	description = "Run unit tests"

	artifactRules = """
		test_results => test_results
		artifacts => artifacts
	""".trimIndent()

	vcs {
		root(WpCalypso)
		cleanCheckout = true
	}

	steps {
		script {
			name = "Prepare environment"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Install modules
				yarn install
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Prevent uncommited changes"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Prevent uncommited changes
				DIRTY_FILES=${'$'}(git status --porcelain 2>/dev/null)
				if [ ! -z "${'$'}DIRTY_FILES" ]; then
					echo "Repository contains uncommitted changes: "
					echo "${'$'}DIRTY_FILES"
					echo "You need to checkout the branch, run 'yarn' and commit those files."
					exit 1
				fi
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Prevent duplicated packages"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				# Duplicated packages
				DUPLICATED_PACKAGES=${'$'}(npx yarn-deduplicate --list)
				if [[ -n "${'$'}DUPLICATED_PACKAGES" ]]; then
					echo "Repository contains duplicated packages: "
					echo ""
					echo "${'$'}DUPLICATED_PACKAGES"
					echo ""
					echo "To fix them, you need to checkout the branch, run 'npx yarn-deduplicate && yarn',"
					echo "verify that the new packages work and commit the changes in 'yarn.lock'."
					exit 1
				else
					echo "No duplicated packages found."
				fi
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run type checks"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Run type checks
				yarn tsc --build packages/tsconfig.json
				yarn tsc --build apps/editing-toolkit/tsconfig.json
				yarn tsc --project client/landing/gutenboarding
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run unit tests for client"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export JEST_JUNIT_OUTPUT_NAME="results.xml"
				unset NODE_ENV
				unset CALYPSO_ENV

				# Run client tests
				JEST_JUNIT_OUTPUT_DIR="./test_results/client" yarn test-client --maxWorkers=${'$'}JEST_MAX_WORKERS --ci --reporters=default --reporters=jest-junit --silent
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run unit tests for server"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export JEST_JUNIT_OUTPUT_NAME="results.xml"
				unset NODE_ENV
				unset CALYPSO_ENV

				# Run server tests
				JEST_JUNIT_OUTPUT_DIR="./test_results/server" yarn test-server --maxWorkers=${'$'}JEST_MAX_WORKERS --ci --reporters=default --reporters=jest-junit --silent
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run unit tests for packages"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export JEST_JUNIT_OUTPUT_NAME="results.xml"
				unset NODE_ENV
				unset CALYPSO_ENV

				# Run packages tests
				JEST_JUNIT_OUTPUT_DIR="./test_results/packages" yarn test-packages --maxWorkers=${'$'}JEST_MAX_WORKERS --ci --reporters=default --reporters=jest-junit --silent
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run unit tests for build tools"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export JEST_JUNIT_OUTPUT_NAME="results.xml"
				unset NODE_ENV
				unset CALYPSO_ENV

				# Run build-tools tests
				JEST_JUNIT_OUTPUT_DIR="./test_results/build-tools" yarn test-build-tools --maxWorkers=${'$'}JEST_MAX_WORKERS --ci --reporters=default --reporters=jest-junit --silent
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Build artifacts"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="production"

				# Build o2-blocks
				(cd apps/o2-blocks/ && yarn build --output-path="../../artifacts/o2-blocks")

				# Build wpcom-block-editor
				(cd apps/wpcom-block-editor/ &&  NODE_ENV=development yarn build --output-path="../../artifacts/wpcom-block-editor")

				# Build notifications
				(cd apps/notifications/ && yarn build --output-path="../../artifacts/notifications")
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Build components storybook"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="production"

				yarn components:storybook:start --ci --smoke-test
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Build search storybook"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="production"

				yarn search:storybook:start --ci --smoke-test
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
	}

	triggers {
		vcs {
			branchFilter = """
				+:*
				-:pull*
			""".trimIndent()
		}
	}

	failureConditions {
		executionTimeoutMin = 10
	}

	features {
		feature {
			type = "xml-report-plugin"
			param("xmlReportParsing.reportType", "junit")
			param("xmlReportParsing.reportDirs", "test_results/**/*.xml")
		}
		perfmon {
		}
		pullRequests {
			vcsRootExtId = "${WpCalypso.id}"
			provider = github {
				authType = token {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
				filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
			}
		}
		commitStatusPublisher {
			vcsRootExtId = "${WpCalypso.id}"
			publisher = github {
				githubUrl = "https://api.github.com"
				authType = personalToken {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
			}
		}

		notifications {
			notifierSettings = slackNotifier {
				connection = "PROJECT_EXT_11"
				sendTo = "#team-calypso-bot"
				messageFormat = simpleMessageFormat()
			}
			branchFilter = """
				+:trunk
			""".trimIndent()
			buildFailedToStart = true
			buildFailed = true
			buildFinishedSuccessfully = true
			firstSuccessAfterFailure = true
			buildProbablyHanging = true
		}
	}
})

object CheckCodeStyle : BuildType({
	name = "Check code style"
	description = "Check code style"

	artifactRules = """
		checkstyle_results => checkstyle_results
	""".trimIndent()

	vcs {
		root(WpCalypso)
		cleanCheckout = true
	}

	steps {
		script {
			name = "Prepare environment"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Install modules
				yarn install
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run linters"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Lint files
				yarn run eslint --format checkstyle --output-file "./checkstyle_results/eslint/results.xml" .
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
	}

	triggers {
		schedule {
			schedulingPolicy = daily {
				hour = 5
			}
			branchFilter = """
				+:trunk
			""".trimIndent()
			triggerBuild = always()
			withPendingChangesOnly = false
		}
	}

	failureConditions {
		executionTimeoutMin = 20
		nonZeroExitCode = false
		failOnMetricChange {
			metric = BuildFailureOnMetric.MetricType.INSPECTION_ERROR_COUNT
			units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
			comparison = BuildFailureOnMetric.MetricComparison.MORE
			compareTo = build {
				buildRule = lastSuccessful()
			}
		}
	}

	features {
		feature {
			type = "xml-report-plugin"
			param("xmlReportParsing.reportType", "checkstyle")
			param("xmlReportParsing.reportDirs", "checkstyle_results/**/*.xml")
			param("xmlReportParsing.verboseOutput", "true")
		}
		perfmon {
		}
	}
})

object CheckCodeStyleBranch : BuildType({
	name = "Check code style for branches"
	description = "Check code style for branches"

	artifactRules = """
		checkstyle_results => checkstyle_results
	""".trimIndent()

	vcs {
		root(WpCalypso)
		cleanCheckout = true
	}

	steps {
		script {
			name = "Prepare environment"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Install modules
				yarn install
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run linters"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Find files to lint
				if [ "%calypso.run_full_eslint%" = "true" ]; then
					FILES_TO_LINT="."
				else
					FILES_TO_LINT=${'$'}(git diff --name-only --diff-filter=d refs/remotes/origin/trunk...HEAD | grep -E '(\.[jt]sx?|\.md)${'$'}' || exit 0)
				fi
				echo "Files to lint:"
				echo ${'$'}FILES_TO_LINT
				echo ""

				# Lint files
				if [ ! -z "${'$'}FILES_TO_LINT" ]; then
					yarn run eslint --format checkstyle --output-file "./checkstyle_results/eslint/results.xml" ${'$'}FILES_TO_LINT
				fi
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image%"
			dockerRunParameters = "-u %env.UID%"
		}
	}

	triggers {
		vcs {
			branchFilter = """
				+:*
				-:trunk
				-:pull*
			""".trimIndent()
		}
	}

	failureConditions {
		executionTimeoutMin = 20
	}

	features {
		feature {
			type = "xml-report-plugin"
			param("xmlReportParsing.reportType", "checkstyle")
			param("xmlReportParsing.reportDirs", "checkstyle_results/**/*.xml")
			param("xmlReportParsing.verboseOutput", "true")
		}
		perfmon {
		}
		pullRequests {
			vcsRootExtId = "${WpCalypso.id}"
			provider = github {
				authType = token {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
				filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
			}
		}
		commitStatusPublisher {
			vcsRootExtId = "${WpCalypso.id}"
			publisher = github {
				githubUrl = "https://api.github.com"
				authType = personalToken {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
			}
		}
	}
})

object RunCalypsoE2eDesktopTests : BuildType({
	uuid = "52f38738-92b2-43cb-b7fb-19fce03cb67c"
	name = "Run browser e2e tests (desktop)"
	description = "Run browser e2e tests (desktop)"

	artifactRules = """
		reports => reports
		logs.tgz => logs.tgz
		screenshots => screenshots
	""".trimIndent()

	vcs {
		root(WpCalypso)
		cleanCheckout = true
	}

	steps {
		script {
			name = "Prepare environment"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export NODE_ENV="test"

				# Install modules
				yarn install
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image_e2e%"
			dockerRunParameters = "-u %env.UID%"
		}
		script {
			name = "Run e2e tests (desktop)"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail
				shopt -s globstar
				set -x

				cd test/e2e
				mkdir temp

				export LIVEBRANCHES=true
				export NODE_CONFIG_ENV=test
				export TEST_VIDEO=true

				# Instructs Magellan to not hide the output from individual `mocha` processes. This is required for
				# mocha-teamcity-reporter to work.
				export MAGELLANDEBUG=true

				function join() {
					local IFS=${'$'}1
					shift
					echo "${'$'}*"
				}

				IMAGE_URL="https://calypso.live?image=registry.a8c.com/calypso/app:build-${BuildDockerImage.depParamRefs.buildNumber}";
				MAX_LOOP=10
				COUNTER=0

				# Transform an URL like https://calypso.live?image=... into https://<container>.calypso.live
				while [[ ${'$'}COUNTER -le ${'$'}MAX_LOOP ]]; do
					COUNTER=${'$'}((COUNTER+1))
					REDIRECT=${'$'}(curl --output /dev/null --silent --show-error  --write-out "%{http_code} %{redirect_url}" "${'$'}{IMAGE_URL}")
					read HTTP_STATUS URL <<< "${'$'}{REDIRECT}"

					# 202 means the image is being downloaded, retry in a few seconds
					if [[ "${'$'}{HTTP_STATUS}" -eq "202" ]]; then
						sleep 5
						continue
					fi

					break
				done

				if [[ -z "${'$'}URL" ]]; then
					echo "Can't redirect to ${'$'}{IMAGE_URL}" >&2
					echo "Curl response: ${'$'}{REDIRECT}" >&2
					exit 1
				fi

				# Decrypt config
				openssl aes-256-cbc -md sha1 -d -in ./config/encrypted.enc -out ./config/local-test.json -k "%CONFIG_E2E_ENCRYPTION_KEY%"

				# Run the test
				export BROWSERSIZE="desktop"
				export BROWSERLOCALE="en"
				export NODE_CONFIG="{\"calypsoBaseURL\":\"${'$'}{URL%/}\"}"
				export TEST_FILES=${'$'}(join ',' ${'$'}(ls -1 specs*/**/*spec.js))

				yarn magellan --config=magellan.json --max_workers=%E2E_WORKERS% --suiteTag=parallel --local_browser=chrome --mocha_args="--reporter mocha-teamcity-reporter" --test=${'$'}{TEST_FILES}
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerImage = "%docker_image_e2e%"
			dockerRunParameters = "-u %env.UID% --security-opt seccomp=.teamcity/docker-seccomp.json --shm-size=8gb"
		}
		script {
			name = "Collect results"
			executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
			scriptContent = """
				#!/bin/bash

				set -o errexit
				set -o nounset
				set -o pipefail
				set -x

				mkdir -p screenshots
				find test/e2e/temp -type f -path '*/screenshots/*' -print0 | xargs -r -0 mv -t screenshots

				mkdir -p logs
				find test/e2e -name '*.log' -print0 | xargs -r -0 tar cvfz logs.tgz
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerImage = "%docker_image_e2e%"
			dockerRunParameters = "-u %env.UID%"
		}
	}

	features {
		perfmon {
		}
		pullRequests {
			vcsRootExtId = "${WpCalypso.id}"
			provider = github {
				authType = token {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
				filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
			}
		}
		commitStatusPublisher {
			vcsRootExtId = "${WpCalypso.id}"
			publisher = github {
				githubUrl = "https://api.github.com"
				authType = personalToken {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
			}
		}
	}

	triggers {
		vcs {
			branchFilter = """
				+:*
				-:trunk
				-:pull*
			""".trimIndent()
		}
	}

	failureConditions {
		executionTimeoutMin = 20
		// TeamCity will mute a test if it fails and then succeeds within the same build. Otherwise TeamCity UI will not
		// display a difference between real errors and retries, making it hard to understand what is actually failing.
		supportTestRetry = true
	}

	dependencies {
		snapshot(BuildDockerImage) {
		}
	}
})


object WpCalypso : GitVcsRoot({
	name = "wp-calypso"
	url = "git@github.com:Automattic/wp-calypso.git"
	pushUrl = "git@github.com:Automattic/wp-calypso.git"
	branch = "refs/heads/trunk"
	branchSpec = "+:refs/heads/*"
	useTagsAsBranches = true
	authMethod = uploadedKey {
		uploadedKey = "Sergio TeamCity"
	}
})

object WpDesktop : Project({
	name = "Desktop app"
	buildType(WpDesktop_DesktopE2ETests)

	params {
		text("docker_image_dekstop", "registry.a8c.com/calypso/ci-desktop:latest", label = "Docker image", description = "Docker image to use for the run", allowEmpty = true)
		password("CALYPSO_SECRETS_ENCRYPTION_KEY", "credentialsJSON:ff451a7d-df79-4635-b6e8-cbd6ec18ddd8", description = "password for encrypting/decrypting certificates and general secrets for the wp-desktop and simplenote-electron repo", display = ParameterDisplay.HIDDEN)
		password("E2EGUTENBERGUSER", "credentialsJSON:27ca9d7b-c6b5-4e84-94d5-ea43879d8184", display = ParameterDisplay.HIDDEN)
		password("E2EPASSWORD", "credentialsJSON:2c4425c4-07d2-414c-9f18-b64da307bdf2", display = ParameterDisplay.HIDDEN)
	}
})

object WpDesktop_DesktopE2ETests : BuildType({
	name = "Run e2e tests"
	description = "Run wp-desktop e2e tests in Linux"

	artifactRules = """
		desktop/release => release
		desktop/e2e/logs => logs
		desktop/e2e/screenshots => screenshots
	""".trimIndent()

	vcs {
		root(WpCalypso)
		cleanCheckout = true
	}

	steps {
		script {
			name = "Prepare environment"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				# Restore mtime to maximize cache hits
				/usr/lib/git-core/git-restore-mtime --force --commit-time --skip-missing

				# Decript certs
				openssl aes-256-cbc -md md5 -d -in desktop/resource/calypso/secrets.json.enc -out config/secrets.json -k "%CALYPSO_SECRETS_ENCRYPTION_KEY%"

				# Install modules
				yarn install
				yarn run build-desktop:install-app-deps
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image_dekstop%"
			dockerRunParameters = "-u %env.UID%"
		}

		script {
			name = "Build Calypso source"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				# Build desktop
				yarn run build-desktop:source
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image_dekstop%"
			dockerRunParameters = "-u %env.UID%"
		}

		script {
			name = "Build app (linux)"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export ELECTRON_BUILDER_ARGS='-c.linux.target=dir'
				export USE_HARD_LINKS=false

				# Build app
				yarn run build-desktop:app
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image_dekstop%"
			dockerRunParameters = "-u %env.UID%"
		}

		script {
			name = "Run tests (linux)"
			scriptContent = """
				#!/bin/bash

				# Update node
				. "${'$'}NVM_DIR/nvm.sh" --no-use
				nvm install

				set -o errexit
				set -o nounset
				set -o pipefail

				export E2EGUTENBERGUSER="%E2EGUTENBERGUSER%"
				export E2EPASSWORD="%E2EPASSWORD%"
				export CI=true

				# Start framebuffer
				Xvfb ${'$'}{DISPLAY} -screen 0 1280x1024x24 &

				# Run tests
				yarn run test-desktop:e2e
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image_dekstop%"
			// See https://stackoverflow.com/a/53975412 and https://blog.jessfraz.com/post/how-to-use-new-docker-seccomp-profiles/
			// TDLR: Chrome needs access to some kernel level operations to create a sandbox, this option unblocks them.
			dockerRunParameters = "-u %env.UID% --security-opt seccomp=.teamcity/docker-seccomp.json"
		}

		script {
			name = "Clean up artifacts"
			executionMode = BuildStep.ExecutionMode.RUN_ON_SUCCESS
			scriptContent = """
				#!/bin/bash
				set -o errexit
				set -o nounset
				set -o pipefail

				# Delete artifacts if branch is not trunk
				if [ "%teamcity.build.branch.is_default%" != "true" ]; then
					rm -fr desktop/release/*
				fi
			""".trimIndent()
			dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
			dockerPull = true
			dockerImage = "%docker_image_dekstop%"
			dockerRunParameters = "-u %env.UID% "
		}
	}

	failureConditions {
		executionTimeoutMin = 15
	}

	features {
		feature {
			type = "xml-report-plugin"
			param("xmlReportParsing.reportType", "junit")
			param("xmlReportParsing.reportDirs", "desktop/e2e/result.xml")
		}
		perfmon {
		}
		pullRequests {
			vcsRootExtId = "${WpCalypso.id}"
			provider = github {
				authType = token {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
				filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
			}
		}
		commitStatusPublisher {
			vcsRootExtId = "${WpCalypso.id}"
			publisher = github {
				githubUrl = "https://api.github.com"
				authType = personalToken {
					token = "credentialsJSON:57e22787-e451-48ed-9fea-b9bf30775b36"
				}
			}
		}

		notifications {
			notifierSettings = slackNotifier {
				connection = "PROJECT_EXT_11"
				sendTo = "#wp-desktop-calypso-e2e"
				messageFormat = verboseMessageFormat {
					addBranch = true
					maximumNumberOfChanges = 10
				}
			}
			buildFailedToStart = true
			buildFailed = true
			buildFinishedSuccessfully = true
			firstSuccessAfterFailure = true
			buildProbablyHanging = true
		}
	}

	triggers {
		vcs {
			branchFilter = """
				+:*
				-:pull*
			""".trimIndent()
		}
	}
})

object WPComPlugins : Project({
	name = "WPCom Plugins"
	buildType(_self.builds.WPComPlugins_EditorToolKit)
	params {
		text("docker_image_wpcom", "registry.a8c.com/calypso/ci-wpcom:latest", label = "Docker image", description = "Docker image to use for the run", allowEmpty = true)
	}
})
