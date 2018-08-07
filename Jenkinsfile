#!/usr/bin/env groovy

@Library('zenreach@master') import static com.zenreach.Environments.*

zenreachPipeline([
    name:       "secor",
    build:      params.build,
    release:    params.release,
    build_prep_script: """

	""",
    build_script: """
         ./build
         ./build deploy
	"""
])
