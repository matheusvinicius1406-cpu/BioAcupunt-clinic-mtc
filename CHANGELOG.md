# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Introduzido gerenciamento explícito de migrations do Room, removendo `fallbackToDestructiveMigration()`.
- Adicionado backup automático do banco antes de cada migration, com retenção dos últimos 5 backups.
- Adicionadas migrations 1→2, 2→3, 3→4, 4→5, 5→6, 6→7, 7→8 com alteração incremental de schema.
