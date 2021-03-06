/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.python.bandit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.python.ExternalIssuesSensor;
import org.sonar.plugins.python.bandit.BanditJsonReportReader.Issue;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.ParseException;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class BanditSensor extends ExternalIssuesSensor {

  private static final Logger LOG = Loggers.get(BanditSensor.class);

  public static final String LINTER_NAME = "Bandit";
  public static final String LINTER_KEY = "bandit";
  public static final String REPORT_PATH_KEY = "sonar.python.bandit.reportPaths";

  private static final Long DEFAULT_CONSTANT_DEBT_MINUTES = 5L;

  @Override
  protected void importReport(File reportPath, SensorContext context, Set<String> unresolvedInputFiles) {
    try (InputStream in = new FileInputStream(reportPath)) {
      LOG.info("Importing {}", reportPath);
      boolean engineIdIsSupported = context.getSonarQubeVersion().isGreaterThanOrEqual(Version.create(7, 4));
      BanditJsonReportReader.read(in, issue -> saveIssue(context, issue, unresolvedInputFiles, engineIdIsSupported));
    } catch (IOException | ParseException | RuntimeException e) {
      LOG.error("No issues information will be saved as the report file '{}' can't be read. " +
        e.getClass().getSimpleName() + ": " + e.getMessage(), reportPath, e);
    }
  }

  private static void saveIssue(SensorContext context, Issue issue, Set<String> unresolvedInputFiles, boolean engineIdIsSupported) {
    if (isEmpty(issue.ruleKey) || isEmpty(issue.filePath) || isEmpty(issue.message)) {
      LOG.debug("Missing information for ruleKey:'{}', filePath:'{}', message:'{}'", issue.ruleKey, issue.filePath, issue.message);
      return;
    }

    InputFile inputFile = context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(issue.filePath));
    if (inputFile == null) {
      unresolvedInputFiles.add(issue.filePath);
      return;
    }

    NewExternalIssue newExternalIssue = context.newExternalIssue();
    newExternalIssue
      .type(RuleType.VULNERABILITY)
      .severity(toSonarQubeSeverity(issue.severity, issue.confidence))
      .remediationEffortMinutes(DEFAULT_CONSTANT_DEBT_MINUTES);

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(issue.message)
      .on(inputFile);

    if (issue.lineNumber != null) {
      primaryLocation.at(inputFile.selectLine(issue.lineNumber));
    }

    newExternalIssue.at(primaryLocation);

    if (engineIdIsSupported) {
      newExternalIssue.engineId(LINTER_KEY).ruleId(issue.ruleKey);
    } else {
      // Call the deprecated "forRule" method to support SQ 7.2
      newExternalIssue.forRule(RuleKey.of(LINTER_KEY, issue.ruleKey));
    }

    newExternalIssue.save();
  }

  private static Severity toSonarQubeSeverity(String severity, String confidence) {
    if ("HIGH".equalsIgnoreCase(severity)) {
      return "HIGH".equalsIgnoreCase(confidence) ? Severity.BLOCKER : Severity.CRITICAL;
    } else if ("MEDIUM".equalsIgnoreCase(severity)) {
      return Severity.MAJOR;
    } else {
      return Severity.MINOR;
    }
  }

  @Override
  protected String linterName() {
    return LINTER_NAME;
  }

  @Override
  protected String reportPathKey() {
    return REPORT_PATH_KEY;
  }

  @Override
  protected Logger logger() {
    return LOG;
  }

}
