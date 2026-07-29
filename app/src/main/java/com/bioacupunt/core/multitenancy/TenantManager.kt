package com.bioacupunt.core.multitenancy

/**
 * Resolve o tenant (clínica) atual — usado por ~15 arquivos: 6 ViewModels
 * (Prescricao, Farmacologia, FarmacologiaCuradoria, Dashboard, Crm,
 * Financeiro) e os repositories deles.
 *
 * Interface, não classe concreta — extraída em 2026-07-29 (achado de auditoria):
 * a implementação real ([TenantManagerImpl]) depende de [com.bioacupunt.security.SecurePreferences],
 * que por sua vez exige um `AndroidKeyStore` real via `MasterKey` — não constrói
 * em JVM puro nem sob Robolectric sem shadow de keystore. Enquanto `TenantManager`
 * era a própria classe concreta, todo consumidor virava, de fato, intestável sem
 * device. Mesmo padrão já usado no resto do projeto: `XRepository` (interface) +
 * `XRepositoryImpl` (implementação) — ver CLAUDE.md.
 */
interface TenantManager {
    fun currentTenantId(defaultTenantId: Long = 1L): Long
    fun setCurrentTenantId(tenantId: Long)
    fun requireTenantId(defaultTenantId: Long = 1L): Long
    fun clear()
}
