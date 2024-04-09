#!/usr/bin/env bats

setup() {
  EXE_BUNDLE_DIR="$BATS_TMPDIR/smithy-image"
  EXE_NAME="smithy"
  SMITHY_EXE_VERSION="1.0.0"
  new_exe_bundle "$SMITHY_EXE_VERSION"
  INSTALL_DIR="$BATS_TMPDIR/smithy"
  BIN_DIR="$BATS_TMPDIR/bin"
  BIN_EXE="$BIN_DIR/$EXE_NAME"
  PREV_PATH=$PATH
  PATH=$PATH:$BIN_DIR
}

teardown() {
  clear_preexisting_bundle
  rm -rf "$INSTALL_DIR"
  rm -rf "$BIN_DIR"
}

new_exe_bundle() {
  smithy_version="$1"
  clear_preexisting_bundle
  mkdir -p "$EXE_BUNDLE_DIR/bin"
  cp "${BATS_TEST_DIRNAME}/install" "$EXE_BUNDLE_DIR"
  create_exes "$smithy_version"
}

clear_preexisting_bundle() {
  [ -d "$EXE_BUNDLE_DIR" ] || rm -rf "$EXE_BUNDLE_DIR"
}

create_exes() {
  smithy_version="$1"
  smithy_exe="$EXE_BUNDLE_DIR/bin/$EXE_NAME"

  echo "echo $smithy_version" > "$smithy_exe"
  chmod +x "$smithy_exe"
}

run_install() {
  run "$EXE_BUNDLE_DIR/install" "$@"
}

assert_expected_installation() {
  expected_installed_version="$1"
  expected_current_dir="$INSTALL_DIR/current"
  expected_install_dir="$INSTALL_DIR/$expected_installed_version"

  # Assert that the installation of the installed version is correct
  [ -d "$expected_install_dir" ]

  # Assert that current points to the expected installed version
  readlink "$expected_current_dir"
  assert_expected_symlink "$expected_current_dir" "$expected_install_dir"

  # Assert the bin symlinks are correct
  assert_expected_symlink "$BIN_EXE" "$expected_current_dir/bin/smithy"

  # Assert the executable works with the expected output
  [ "$("$BIN_EXE" --version)" = "$expected_installed_version" ]
}

assert_expected_symlink() {
  expected_symlink="$1"
  expected_target="$2"
  [ -L "$expected_symlink" ]
  [ "$(readlink "$expected_symlink")" = "$expected_target" ]
}

@test "-h prints help" {
  run_install -h
  [[ "$output" = *"USAGE"* ]]
}

@test "--help prints help" {
  run_install -h
  [[ "$output" = *"USAGE"* ]]
}

@test "with unexpected arguments" {
  run_install --unexpected
  [ "$status" -eq 1 ]
  [[ "$output" = *"unexpected argument"* ]]
}

@test "install" {
  run_install --install-dir "$INSTALL_DIR" --bin-dir "$BIN_DIR"
  [ "$status" -eq 0 ]
  echo $output
  [[ "$output" == *"You can now run: '$BIN_EXE --version' or '$EXE_NAME --version'"* ]]
  echo $output
  assert_expected_installation "$SMITHY_EXE_VERSION"
}

@test "using shorthand parameters" {
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR"
  [ "$status" -eq 0 ]
  [[ "$output" == *"You can now run: '$BIN_EXE --version' or '$EXE_NAME --version'"* ]]
  assert_expected_installation "$SMITHY_EXE_VERSION"
}

@test "fails when detects preexisting installation" {
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR"
  [ "$status" -eq 0 ]

  new_exe_bundle "new-version"
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR"
  [ "$status" -eq 1 ]
  [[ "$output" = *"Found preexisting"* ]]
}

@test "--updates updates to new version" {
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR"
  [ "$status" -eq 0 ]

  new_exe_bundle "new-version"
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR" --update
  [ "$status" -eq 0 ]
  assert_expected_installation "new-version"
}

@test "-u updates to new version" {
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR"
  [ "$status" -eq 0 ]

  new_exe_bundle "new-version"
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR" -u
  [ "$status" -eq 0 ]
  assert_expected_installation "new-version"
}

@test "--update skips for same version" {
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR"
  [ "$status" -eq 0 ]

  run_install -i "$INSTALL_DIR" -b "$BIN_DIR" -u
  [ "$status" -eq 0 ]
  [[ "$output" = *"Found same Smithy CLI version"* ]]
  assert_expected_installation "$SMITHY_EXE_VERSION"
}

@test "detects PATH missing BIN_DIR" {
  PATH=$PREV_PATH
  run_install -i "$INSTALL_DIR" -b "$BIN_DIR"
  [ "$status" -eq 0 ]
  [[ "$output" == *"not detected in \$PATH"* ]]
  assert_expected_installation "$SMITHY_EXE_VERSION"
}
