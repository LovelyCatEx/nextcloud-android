## Script from https://github.com/tir38/android-lint-entropy-reducer at 07.05.2017
# adapts to drone, use git username / token as parameter

# TODO cleanup this script, it has a lot of unused stuff

# SPDX-FileCopyrightText: 2017-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2017 Jason Atwood 
# SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only


Encoding.default_external = Encoding::UTF_8
Encoding.default_internal = Encoding::UTF_8

puts "=================== starting Android Lint Entropy Reducer ===================="

# ========================  SETUP ============================

# User name for git commits made by this script.
TRAVIS_GIT_USERNAME = String.new("Drone CI server")

# File name and relative path of generated Lint report. Must match build.gradle file:
#   lintOptions {
#       htmlOutput file("[FILE_NAME].html")
#   }
LINT_REPORT_FILE = String.new("app/build/reports/lint/lint.html")

# File name and relative path of previous results of this script.
PREVIOUS_LINT_RESULTS_FILE=String.new("scripts/analysis/lint-results.txt")

# Flag to evaluate warnings. true = check warnings; false = ignore warnings
CHECK_WARNINGS = true

# File name and relative path to custom lint rules; Can be null or "".
CUSTOM_LINT_FILE = String.new("")

# ================ SETUP DONE; DON'T TOUCH ANYTHING BELOW  ================

require 'fileutils'
require 'pathname'
require 'open3'

# since we need the xml-simple gem, and we want this script self-contained, let's grab it just when we need it
begin
    gem "xml-simple"
    rescue LoadError
    system("gem install --user-install xml-simple")
    Gem.clear_paths
end

require 'xmlsimple'

# add custom Lint jar
if !CUSTOM_LINT_FILE.nil? &&
    CUSTOM_LINT_FILE.length > 0

    ENV["ANDROID_LINT_JARS"] = Dir.pwd + "/" + CUSTOM_LINT_FILE
    puts "adding custom lint rules to default set: "
    puts ENV["ANDROID_LINT_JARS"]
end

# run Lint
puts "running Lint..."
system './gradlew clean lintGplayDebug'

# confirm that Lint ran w/out error
result = $?.to_i
if result != 0
    puts "FAIL: failed to run ./gradlew clean lintGplayDebug"
    exit 1
end

# find Lint report file
lint_reports = Dir.glob(LINT_REPORT_FILE)
if lint_reports.length == 0
    puts "Lint HTML report not found."
    exit 1
end
lint_report = String.new(lint_reports[0])

# find error/warning count string in HTML report
error_warning_string = ""
File.open lint_report do |file|
  error_warning_string = file.find { |line| line =~ /([0-9]* error[s]? and )?[0-9]* warning[s]?/ }
end

# find number of errors
error_string = error_warning_string.match(/[0-9]* error[s]?/)

if (error_string.nil?)
    current_error_count = 0
else
    current_error_count = error_string[0].match(/[0-9]*/)[0].to_i
end

puts "found errors: " + current_error_count.to_s

# find number of warnings
if CHECK_WARNINGS == true
    warning_string = error_warning_string.match(/[0-9]* warning[s]?/)[0]
    current_warning_count = warning_string.match(/[0-9]*/)[0].to_i
    puts "found warnings: " + current_warning_count.to_s
end

# get previous error and warning counts from last successful build

previous_results = false

previous_lint_reports = Dir.glob(PREVIOUS_LINT_RESULTS_FILE)
if previous_lint_reports.nil? ||
    previous_lint_reports.length == 0

    previous_lint_report = File.new(PREVIOUS_LINT_RESULTS_FILE, "w") # create for writing to later
else
    previous_lint_report = String.new(previous_lint_reports[0])

    previous_error_warning_string = ""
    File.open previous_lint_report do |file|
      previous_error_warning_string = file.find { |line| line =~ /([0-9]* error[s]? and )?[0-9]* warning[s]?/ }
    end

    unless previous_error_warning_string.nil?
        previous_results = true

        previous_error_string = previous_error_warning_string.match(/[0-9]* error[s]?/)
        if previous_error_string.nil?
            previous_error_string = "0 errors"
        else
            previous_error_string = previous_error_string[0]
        end
        previous_error_count = previous_error_string.match(/[0-9]*/)[0].to_i
        puts "previous errors: " + previous_error_count.to_s

        if CHECK_WARNINGS == true
            previous_warning_string = previous_error_warning_string.match(/[0-9]* warning[s]?/)
            if previous_warning_string.nil?
                previous_warning_string = "0 warnings"
            else
                previous_warning_string = previous_warning_string[0]
            end
            previous_warning_count = previous_warning_string.match(/[0-9]*/)[0].to_i
            puts "previous warnings: " + previous_warning_count.to_s
        end
    end
end

# compare previous error count with current error count
if previous_results == true  &&
    current_error_count > previous_error_count
    puts "FAIL: error count increased"
    exit 1
end

# compare previous warning count with current warning count
if CHECK_WARNINGS  == true &&
    previous_results == true &&
    current_warning_count > previous_warning_count

    puts "FAIL: warning count increased"
    exit 1
end

# check if warning and error count stayed the same
if  previous_results == true &&
    current_error_count == previous_error_count &&
    current_warning_count == previous_warning_count

    puts "SUCCESS: count stayed the same"
    exit 2
end

# either error count or warning count DECREASED

# write new results to file (will overwrite existing, or create new)
File.write(previous_lint_report, "DO NOT TOUCH; GENERATED BY DRONE\n" + error_warning_string)

# update git user name and email for this script
system ("git config --local user.name 'github-actions'")
system ("git config --local user.email 'github-actions@github.com'")

# add previous Lint result file to git
system ('git add ' + PREVIOUS_LINT_RESULTS_FILE)

# commit changes
system('git commit -sm "Analysis: update lint results to reflect reduced error/warning count"')

# push to origin
system ('git push')

puts "SUCCESS: count was reduced"
exit 0 # success
