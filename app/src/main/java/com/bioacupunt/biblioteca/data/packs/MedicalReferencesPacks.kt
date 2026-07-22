package com.bioacupunt.biblioteca.data.packs

import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentItem
import com.bioacupunt.biblioteca.domain.ingestion.LibraryContentPack

/**
 * PACOTE DE REFERÊNCIAS MÉDICAS — 100 links confiáveis para consulta.
 *
 * Todas as fontes são verificáveis: cada item tem URL pública (sourceUrl)
 * e localizador específico (sourceRef = categoria/nome do recurso),
 * portanto o filtro de proveniência classifica como VERIFICAVEL.
 *
 * Organizado em 13 categorias: OMS, bibliotecas digitais, livros gratuitos,
 * universidades, periódicos, recursos adicionais, MS Brasil, ANVISA,
 * farmácia, sociedades médicas, diretrizes internacionais, periódicos
 * de alto impacto, e recursos educacionais.
 */
object MedicalReferencesPacks {

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 1: ORGANIZAÇÕES MUNDIAIS DE SAÚDE (1-9)
    // ═══════════════════════════════════════════════════════════════

    private val whoPack = LibraryContentPack(
        source = "OMS, CDC, NIH — organizações mundiais de saúde pública e pesquisa biomédica",
        items = listOf(
            LibraryContentItem(
                id = "ref_001", title = "World Health Organization (WHO) — Site oficial",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes, publicações técnicas, estatísticas e relatórios anuais de saúde global.",
                content = "A Organização Mundial da Saúde (OMS/WHO) é a autoridade máxima em saúde global. Publica diretrizes baseadas em evidências, estatísticas sanitárias (GHO), relatórios anuais (World Health Report), classificações internacionais (CID-11), e normas técnicas para prevenção, diagnóstico e tratamento de doenças em escala mundial. Acesso gratuito a milhares de documentos técnicos.",
                tags = listOf("oms", "who", "saúde global", "diretrizes", "organização internacional"),
                citation = "World Health Organization", sourceUrl = "https://www.who.int", sourceRef = "Portal Principal"
            ),
            LibraryContentItem(
                id = "ref_002", title = "WHO Guidelines — Diretrizes da OMS",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes baseadas em evidências para prevenção, diagnóstico e tratamento.",
                content = "Portal de diretrizes clínicas da OMS. Inclui guidelines para doenças transmissíveis (HIV, tuberculose, malária), doenças não transmissíveis (diabetes, hipertensão, câncer), saúde materno-infantil, nutrição, saúde mental, e emergências sanitárias. Todos os documentos são revisados por comitês de especialistas globais e atualizados periodicamente.",
                tags = listOf("oms", "diretrizes", "guias", "protocolos"),
                citation = "World Health Organization", sourceUrl = "https://www.who.int/publications/guidelines", sourceRef = "Seção Guidelines"
            ),
            LibraryContentItem(
                id = "ref_003", title = "WHO IRIS — Repositório Institucional da OMS",
                category = "CLINICA_MEDICA",
                summary = "Repositório digital com milhares de publicações da OMS para download gratuito em PDF.",
                content = "O IRIS (Institutional Repository for Information Sharing) é o repositório digital da OMS. Contém mais de 150.000 publicações técnicas, relatórios de comitês, atas de assembleias mundiais de saúde, resoluções, e documentos históricos. Acesso aberto e download gratuito de PDFs. Cobre todas as áreas de saúde pública global desde 1948.",
                tags = listOf("oms", "repositório", "publicações", "pdf", "acervo"),
                citation = "World Health Organization", sourceUrl = "https://iris.who.int", sourceRef = "Repositório IRIS"
            ),
            LibraryContentItem(
                id = "ref_004", title = "OPAS (PAHO) — Organização Pan-Americana da Saúde",
                category = "CLINICA_MEDICA",
                summary = "Escritório regional da OMS para as Américas. Publicações em português.",
                content = "A Organização Pan-Americana da Saúde (OPAS/PAHO) é o braço da OMS nas Américas. Publica diretrizes, protocolos e relatórios em português, espanhol e inglês. Foco em: atenção primária, vigilância epidemiológica, imunização, doenças tropicais negligenciadas, saúde materno-infantil, e resposta a emergências na região das Américas.",
                tags = listOf("opas", "paho", "américas", "saúde pública", "português"),
                citation = "Pan American Health Organization", sourceUrl = "https://www.paho.org", sourceRef = "Portal Principal OPAS"
            ),
            LibraryContentItem(
                id = "ref_005", title = "PAHO IRIS — Repositório Digital da OPAS",
                category = "CLINICA_MEDICA",
                summary = "Acesso gratuito a publicações da OPAS na região das Américas.",
                content = "Repositório institucional da OPAS com publicações técnicas, diretrizes regionais, relatórios de vigilância, e documentos de cooperação técnica entre países das Américas. Conteúdo em português, espanhol, inglês e francês. Inclui séries históricas de dados de saúde das Américas.",
                tags = listOf("opas", "repositório", "publicações", "américas"),
                citation = "Pan American Health Organization", sourceUrl = "https://iris.paho.org", sourceRef = "Repositório IRIS OPAS"
            ),
            LibraryContentItem(
                id = "ref_006", title = "CDC — Centers for Disease Control and Prevention",
                category = "CLINICA_MEDICA",
                summary = "Agência dos EUA para vigilância e prevenção de doenças.",
                content = "O CDC é a principal agência de saúde pública dos Estados Unidos. Publica: MMWR (Morbidity and Mortality Weekly Report), Emerging Infectious Diseases, diretrizes de prevenção e controle de infecções, recomendações de vacinação (ACIP), guias de vigilância epidemiológica, estatísticas de saúde (NCHS), e respostas a surtos. Grande parte do conteúdo é de acesso aberto e gratuito.",
                tags = listOf("cdc", "vigilância", "prevenção", "epidemiologia", "vacinação"),
                citation = "Centers for Disease Control and Prevention", sourceUrl = "https://www.cdc.gov", sourceRef = "Portal Principal CDC"
            ),
            LibraryContentItem(
                id = "ref_007", title = "CDC Stacks — Repositório Digital do CDC",
                category = "CLINICA_MEDICA",
                summary = "Acesso gratuito a todas as publicações científicas da agência CDC.",
                content = "O CDC Stacks é o repositório institucional do CDC. Reúne todas as publicações da agência: MMWR, diretrizes de saúde pública, relatórios de investigação de surtos, documentos de vigilância, manuais de laboratório, e materiais educacionais. Pesquisa avançada por autor, assunto, data e tipo de documento. Download gratuito.",
                tags = listOf("cdc", "repositório", "publicações", "mmwr"),
                citation = "Centers for Disease Control and Prevention", sourceUrl = "https://stacks.cdc.gov", sourceRef = "Repositório CDC Stacks"
            ),
            LibraryContentItem(
                id = "ref_008", title = "NIH — National Institutes of Health",
                category = "CLINICA_MEDICA",
                summary = "Principal agência de pesquisa biomédica dos EUA.",
                content = "O NIH é a maior fonte de financiamento de pesquisa biomédica do mundo. Portal central para 27 institutos e centros de pesquisa. Oferece: PubMed (base de citações), ClinicalTrials.gov (registro de ensaios clínicos), MedlinePlus (informação ao paciente), e recursos de educação em saúde. Política de acesso aberto obrigatória para todas as publicações financiadas pelo NIH.",
                tags = listOf("nih", "pesquisa biomédica", "financiamento", "acesso aberto"),
                citation = "National Institutes of Health", sourceUrl = "https://www.nih.gov", sourceRef = "Portal Principal NIH"
            ),
            LibraryContentItem(
                id = "ref_009", title = "NLM — National Library of Medicine",
                category = "CLINICA_MEDICA",
                summary = "Biblioteca nacional de medicina dos EUA. Gerencia PubMed, PMC e Bookshelf.",
                content = "A NLM é a maior biblioteca médica do mundo. Opera sistemas de informação críticos para a prática clínica e pesquisa: PubMed (30M+ citações), PubMed Central (PMC, 10M+ artigos completos), NCBI Bookshelf (13.000+ livros), MedlinePlus, ClinicalTrials.gov, e o banco de dados de genomas (GenBank). Pioneira em acesso aberto à literatura científica.",
                tags = listOf("nlm", "pubmed", "biblioteca", "literatura médica"),
                citation = "National Library of Medicine", sourceUrl = "https://www.nlm.nih.gov", sourceRef = "Portal Principal NLM"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 2: BIBLIOTECAS DIGITAIS E BASES DE DADOS (10-24)
    // ═══════════════════════════════════════════════════════════════

    private val digitalLibrariesPack = LibraryContentPack(
        source = "PubMed, Cochrane, DOAJ, TRIP, DynaMed — bibliotecas digitais e bases de evidências",
        items = listOf(
            LibraryContentItem(
                id = "ref_010", title = "PubMed Central (PMC) — Artigos completos gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Arquivo gratuito com mais de 10 milhões de artigos completos de ciências biomédicas.",
                content = "O PubMed Central (PMC) é o arquivo de acesso aberto da NLM/NIH. Diferente do PubMed (que contém apenas citações e resumos), o PMC disponibiliza o texto completo dos artigos em formato XML e PDF. Mais de 10 milhões de artigos, cobertura desde o ano 1800. Integrado com PubMed e NCBI Bookshelf.",
                tags = listOf("pubmed central", "pmc", "artigos", "acesso aberto", "texto completo"),
                citation = "National Library of Medicine", sourceUrl = "https://pmc.ncbi.nlm.nih.gov", sourceRef = "Portal PMC"
            ),
            LibraryContentItem(
                id = "ref_011", title = "PubMed — Base de dados de citações biomédicas",
                category = "CLINICA_MEDICA",
                summary = "Mais de 30 milhões de citações e resumos de artigos biomédicos.",
                content = "O PubMed é a base de dados mais utilizada mundialmente para pesquisa em ciências da saúde. Cobre MEDLINE (artigos indexados com MeSH), PubMed Central (artigos completos), e livros do NCBI Bookshelf. Mais de 30 milhões de registros. Links para texto completo quando disponível. Suporta pesquisa clínica por filtros (ensaio clínico, revisão, meta-análise).",
                tags = listOf("pubmed", "citações", "medline", "mesh", "pesquisa"),
                citation = "National Library of Medicine", sourceUrl = "https://pubmed.ncbi.nlm.nih.gov", sourceRef = "Portal PubMed"
            ),
            LibraryContentItem(
                id = "ref_012", title = "NCBI Bookshelf — Livros e documentos acadêmicos gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Mais de 13.000 livros e documentos acadêmicos gratuitos em ciências da saúde.",
                content = "O NCBI Bookshelf é uma plataforma de livros e documentos de referência em ciências biomédicas e da vida. Inclui livros-texto (Molecular Biology of the Cell, Goodman & Gilman's), relatórios do Institute of Medicine, diretrizes clínicas (NICE, AHRQ), e manuais técnicos (StatPearls). Acesso gratuito com formatação responsiva para leitura online.",
                tags = listOf("bookshelf", "livros", "ncbi", "referência", "gratuito"),
                citation = "National Library of Medicine", sourceUrl = "https://www.ncbi.nlm.nih.gov/books", sourceRef = "NCBI Bookshelf"
            ),
            LibraryContentItem(
                id = "ref_013", title = "MedlinePlus — Informação de saúde para pacientes",
                category = "CLINICA_MEDICA",
                summary = "Informações de saúde confiáveis para o público, em inglês e espanhol.",
                content = "MedlinePlus é um serviço da NLM que oferece informações de saúde de alta qualidade para pacientes e familiares. Conteúdo revisado por especialistas da NLM e NIH. Inclui: enciclopédia médica ilustrada, dicionário médico, guias de medicamentos (herbais e suplementos), vídeos de procedimentos, e informações sobre ensaios clínicos. Disponível em inglês e espanhol.",
                tags = listOf("medlineplus", "paciente", "saúde", "educação", "nlm"),
                citation = "National Library of Medicine", sourceUrl = "https://medlineplus.gov", sourceRef = "Portal MedlinePlus"
            ),
            LibraryContentItem(
                id = "ref_014", title = "Cochrane Library — Revisões sistemáticas",
                category = "CLINICA_MEDICA",
                summary = "Padrão ouro para revisões sistemáticas e ensaios clínicos randomizados.",
                content = "A Biblioteca Cochrane é a principal fonte mundial de revisões sistemáticas de alta qualidade. As revisões Cochrane são o padrão ouro para síntese de evidências em saúde, seguindo metodologia rigorosa (Cochrane Handbook). Inclui: Cochrane Database of Systematic Reviews (CDSR), Central Register of Controlled Trials (CENTRAL), e Methodological Reviews. Resumos em português disponíveis.",
                tags = listOf("cochrane", "revisão sistemática", "evidência", "meta-análise"),
                citation = "The Cochrane Collaboration", sourceUrl = "https://www.cochranelibrary.com", sourceRef = "Cochrane Library"
            ),
            LibraryContentItem(
                id = "ref_015", title = "Epistemonikos — Banco de evidências multilingue",
                category = "CLINICA_MEDICA",
                summary = "Base de dados multilingue gratuita com revisões sistemáticas e resumos de evidências.",
                content = "Epistemonikos é um banco de dados colaborativo e multilingue (inclui português) que reúne revisões sistemáticas, ensaios clínicos e resumos estruturados de evidências. Diferencia-se por classificar os estudos por tipo de evidência e conectar artigos relacionados (matriz de evidências). Ferramenta gratuita para tomada de decisão baseada em evidências.",
                tags = listOf("epistemonikos", "evidência", "revisão", "base de dados", "multilíngue"),
                citation = "Epistemonikos Foundation", sourceUrl = "https://www.epistemonikos.org", sourceRef = "Portal Epistemonikos"
            ),
            LibraryContentItem(
                id = "ref_016", title = "DOAJ — Directory of Open Access Journals",
                category = "CLINICA_MEDICA",
                summary = "Diretório com mais de 20.000 periódicos científicos de acesso aberto.",
                content = "O DOAJ é um diretório curado de periódicos científicos de acesso aberto revisados por pares. Mais de 20.000 periódicos e 10 milhões de artigos. Critérios rigorosos de inclusão: revisão por pares, acesso aberto imediato, licenciamento claro (CC-BY, etc.), e qualidade editorial. Filtros por área (medicina, enfermagem, saúde pública) e país.",
                tags = listOf("doaj", "periódicos", "acesso aberto", "diretório"),
                citation = "DOAJ", sourceUrl = "https://doaj.org", sourceRef = "Portal DOAJ"
            ),
            LibraryContentItem(
                id = "ref_017", title = "HINARI (WHO) — Programa de acesso a periódicos",
                category = "CLINICA_MEDICA",
                summary = "Programa da OMS que oferece acesso gratuito a periódicos biomédicos para países em desenvolvimento.",
                content = "O HINARI (Health InterNetwork Access to Research Initiative) é um programa da OMS que oferece acesso gratuito ou de baixo custo a milhares de periódicos biomédicos para instituições em países de baixa e média renda. Em parceria com editoras como Elsevier, Springer, Wiley e BMJ. Instituições brasileiras podem se qualificar para acesso gratuito.",
                tags = listOf("hinari", "oms", "acesso", "periódicos", "países desenvolvimento"),
                citation = "World Health Organization", sourceUrl = "https://www.who.int/hinari", sourceRef = "Programa HINARI"
            ),
            LibraryContentItem(
                id = "ref_018", title = "TRIP Database — Busca de evidências clínicas",
                category = "CLINICA_MEDICA",
                summary = "Mecanismo de busca clínico que encontra evidências de alta qualidade e diretrizes.",
                content = "O TRIP (Turning Research Into Practice) Database é um mecanismo de busca clínico que indexa fontes de evidência de alta qualidade: Cochrane, PubMed, NICE, SIGN, AHRQ, diretrizes de sociedades médicas, e revistas de alto impacto. Filtros por tipo de estudo, especialidade e data. Versão gratuita disponível com funcionalidades básicas de busca e acesso a resumos.",
                tags = listOf("trip database", "busca clínica", "evidência", "diretrizes"),
                citation = "Trip Database Ltd", sourceUrl = "https://www.tripdatabase.com", sourceRef = "Portal TRIP Database"
            ),
            LibraryContentItem(
                id = "ref_019", title = "OpenEvidence — IA médica com evidências",
                category = "CLINICA_MEDICA",
                summary = "Plataforma de IA médica que sintetiza evidências de literatura revisada com citações transparentes.",
                content = "OpenEvidence é uma plataforma de inteligência artificial projetada para profissionais de saúde. Diferencia-se por sintetizar respostas baseadas exclusivamente em literatura revisada por pares, com citações transparentes e links diretos para as fontes originais. Cobre todas as especialidades médicas. Ideal para suporte à decisão clínica baseado em evidências atualizadas.",
                tags = listOf("open evidence", "ia médica", "evidência", "perguntas clínicas"),
                citation = "OpenEvidence", sourceUrl = "https://www.openevidence.com", sourceRef = "Portal OpenEvidence"
            ),
            LibraryContentItem(
                id = "ref_020", title = "ClinicalKey (Elsevier) — Referência clínica",
                category = "CLINICA_MEDICA",
                summary = "Plataforma de referência clínica com livros, periódicos, vídeos e diretrizes.",
                content = "ClinicalKey é uma plataforma de referência clínica da Elsevier que integra livros-texto (Goldman's Cecil, Netter's), periódicos (The Lancet, Journal of the American College of Cardiology), vídeos de procedimentos, diretrizes, e imagens médicas. Busca inteligente com sinônimos e conceitos relacionados. Acesso geralmente institucional.",
                tags = listOf("clinical key", "elsevier", "referência", "livros", "diretrizes"),
                citation = "Elsevier", sourceUrl = "https://www.elsevier.com/clinical-solutions/clinicalkey", sourceRef = "ClinicalKey"
            ),
            LibraryContentItem(
                id = "ref_021", title = "Essential Evidence Plus — Suporte à decisão clínica",
                category = "CLINICA_MEDICA",
                summary = "Ferramenta com mais de 13.000 tópicos, diretrizes e resumos de evidências.",
                content = "Essential Evidence Plus (EE+) é uma ferramenta de suporte à decisão clínica desenvolvida pelo grupo Wiley. Oferece: mais de 13.000 tópicos clínicos organizados por especialidade, calculadoras médicas (MDCalc integrado), diretrizes clínicas, resumos de evidências (POEMs — Patient-Oriented Evidence that Matters), e monografias de medicamentos. Atualizada semanalmente.",
                tags = listOf("essential evidence plus", "decisão clínica", "tópicos", "evidência"),
                citation = "Wiley", sourceUrl = "https://www.essentialevidenceplus.com", sourceRef = "Portal EE+"
            ),
            LibraryContentItem(
                id = "ref_022", title = "DynaMed — Referência clínica baseada em evidências",
                category = "CLINICA_MEDICA",
                summary = "Referência clínica de ponto de cuidado com 3.400 tópicos atualizados diariamente.",
                content = "DynaMed é uma referência clínica baseada em evidências para o ponto de cuidado (point-of-care). Mais de 3.400 tópicos organizados por especialidade, cada um com níveis de evidência (LOE) classificados. Atualização diária baseada na vigilância sistemática de 400+ periódicos. Integração com EMR. Acesso institucional (EBSCO). Versão gratuita limitada disponível em algumas regiões.",
                tags = listOf("dynamed", "point-of-care", "evidência", "tópicos clínicos"),
                citation = "EBSCO Health", sourceUrl = "https://www.dynamed.com", sourceRef = "Portal DynaMed"
            ),
            LibraryContentItem(
                id = "ref_023", title = "BMJ Best Practice — Guia clínico passo a passo",
                category = "CLINICA_MEDICA",
                summary = "Guia clínico baseado em evidências do British Medical Journal.",
                content = "BMJ Best Practice é um guia clínico passo a passo desenvolvido pelo British Medical Journal. Abrange diagnóstico, tratamento e prognóstico de mais de 1.000 condições clínicas. Conteúdo estruturado por etapas: anamnese, exame físico, exames complementares, diagnóstico diferencial, tratamento (primeira linha, segunda linha), e acompanhamento. Integrado com evidências do BMJ Clinical Evidence.",
                tags = listOf("bmj best practice", "guia clínico", "passo a passo", "diagnóstico"),
                citation = "British Medical Journal", sourceUrl = "https://bestpractice.bmj.com", sourceRef = "Portal BMJ Best Practice"
            ),
            LibraryContentItem(
                id = "ref_024", title = "Free Medical Journals — Periódicos gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Diretório com mais de 2.000 periódicos científicos gratuitos.",
                content = "Free Medical Journals é um diretório que cataloga periódicos científicos com acesso gratuito ao texto completo. Mais de 2.000 periódicos organizados por especialidade médica. Inclui desde periódicos de acesso aberto total até aqueles com artigos gratuitos selecionados. Ferramenta útil para identificar onde ler artigos sem custo.",
                tags = listOf("free medical journals", "periódicos", "gratuito", "diretório"),
                citation = "Free Medical Journals", sourceUrl = "https://www.freemedicaljournals.com", sourceRef = "Portal Free Medical Journals"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 3: LIVROS MÉDICOS GRATUITOS (25-31)
    // ═══════════════════════════════════════════════════════════════

    private val freeBooksPack = LibraryContentPack(
        source = "NCBI Bookshelf, NAP, FreeBooks4Doctors, Internet Archive — livros médicos gratuitos online",
        items = listOf(
            LibraryContentItem(
                id = "ref_025", title = "NCBI Bookshelf — Livros didáticos de biomedicina",
                category = "CLINICA_MEDICA",
                summary = "Mais de 13.000 livros didáticos e manuais de referência em ciências biomédicas.",
                content = "O NCBI Bookshelf oferece mais de 13.000 livros-texto e documentos de referência em ciências biomédicas e da saúde. Inclui clássicos como: Molecular Biology of the Cell (Alberts), Goodman & Gilman's The Pharmacological Basis of Therapeutics, e StatPearls. Todos gratuitos, formatados para leitura online responsiva, com links para artigos do PubMed.",
                tags = listOf("bookshelf", "livros", "biomedicina", "gratuito", "ncbi"),
                citation = "National Library of Medicine", sourceUrl = "https://www.ncbi.nlm.nih.gov/books", sourceRef = "NCBI Bookshelf"
            ),
            LibraryContentItem(
                id = "ref_026", title = "National Academies Press — PDFs gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Mais de 8.500 títulos em PDF para download gratuito sobre ciência e medicina.",
                content = "A National Academies Press (NAP) publica os relatórios das National Academies of Sciences, Engineering, and Medicine dos EUA. Mais de 8.500 títulos em PDF para download gratuito. Abrange: saúde pública, políticas de saúde, nutrição, segurança do paciente, e inovação em medicina. Relatórios encomendados pelo governo dos EUA com alto rigor técnico.",
                tags = listOf("national academies", "nap", "pdf", "relatórios", "gratuito"),
                citation = "National Academies of Sciences", sourceUrl = "https://www.nap.edu", sourceRef = "Portal NAP"
            ),
            LibraryContentItem(
                id = "ref_027", title = "FreeBooks4Doctors — Livros médicos gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Diretório com mais de 370 livros médicos gratuitos disponíveis online.",
                content = "FreeBooks4Doctors é um diretório selecionado de livros médicos gratuitos disponíveis na internet. Organizado por especialidade (cardiologia, dermatologia, neurologia, pediatria, etc.). Links para textos completos de livros clássicos e contemporâneos. Curadoria manual garante que os links estejam ativos e o conteúdo seja relevante para a prática clínica.",
                tags = listOf("freebooks4doctors", "livros", "gratuito", "diretório"),
                citation = "FreeBooks4Doctors", sourceUrl = "https://www.freebooks4doctors.com", sourceRef = "Portal FreeBooks4Doctors"
            ),
            LibraryContentItem(
                id = "ref_028", title = "Free Medical Books — Livros por especialidade",
                category = "CLINICA_MEDICA",
                summary = "Portal com centenas de livros médicos gratuitos organizados por especialidade.",
                content = "Free Medical Books é um portal que reúne links para centenas de livros médicos gratuitos, organizados por especialidade. Navegação fácil com categorias como: anatomia, fisiologia, patologia, farmacologia, cirurgia, clínica médica, pediatria, ginecologia e obstetrícia. Inclui atlas, manuais e livros-texto completos.",
                tags = listOf("free medical books", "livros", "especialidade", "gratuito"),
                citation = "Free Medical Books", sourceUrl = "https://www.freemedicalbooks.com", sourceRef = "Portal Free Medical Books"
            ),
            LibraryContentItem(
                id = "ref_029", title = "E-Books Directory — Livros científicos gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Diretório com links para livros médicos e científicos gratuitos.",
                content = "E-Books Directory é um diretório que cataloga livros científicos e médicos disponíveis gratuitamente online. Foco em livros-texto de nível universitário e de pós-graduação. Categorias: medicina, biologia, química, bioquímica, genética, microbiologia, e neurociências. Links verificados para download direto em PDF.",
                tags = listOf("e-books directory", "livros", "científicos", "gratuito", "diretório"),
                citation = "E-Books Directory", sourceUrl = "https://www.e-booksdirectory.com/medical.php", sourceRef = "Seção Medicina"
            ),
            LibraryContentItem(
                id = "ref_030", title = "Internet Archive — Biblioteca digital",
                category = "CLINICA_MEDICA",
                summary = "Biblioteca digital sem fins lucrativos com milhões de livros e coleções médicas.",
                content = "O Internet Archive é uma biblioteca digital sem fins lucrativos que oferece acesso gratuito a milhões de livros, filmes, músicas e websites. Inclui coleções médicas históricas e modernas, livros de medicina em domínio público, e o Wayback Machine (arquivo histórico de websites). A coleção 'Medical Heritage Library' reúne livros raros de medicina dos séculos XIX e XX.",
                tags = listOf("internet archive", "biblioteca digital", "livros", "história da medicina"),
                citation = "Internet Archive", sourceUrl = "https://archive.org", sourceRef = "Portal Internet Archive"
            ),
            LibraryContentItem(
                id = "ref_031", title = "Project Gutenberg — Clássicos da medicina",
                category = "CLINICA_MEDICA",
                summary = "Mais de 60.000 eBooks gratuitos, incluindo clássicos da medicina em domínio público.",
                content = "O Project Gutenberg oferece mais de 60.000 eBooks gratuitos de obras em domínio público. A coleção médica inclui clássicos: Osier's Principles and Practice of Medicine (edições históricas), Gray's Anatomy (ilustrações originais), Darwin, Freud, Hipócrates, e textos fundadores da medicina ocidental. Formatos: ePub, Kindle, HTML e texto puro.",
                tags = listOf("project gutenberg", "e-books", "clássicos", "domínio público", "medicina"),
                citation = "Project Gutenberg", sourceUrl = "https://www.gutenberg.org", sourceRef = "Portal Project Gutenberg"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 4: UNIVERSIDADES E INSTITUIÇÕES ACADÊMICAS (32-46)
    // ═══════════════════════════════════════════════════════════════

    private val universityPack = LibraryContentPack(
        source = "Harvard, Johns Hopkins, Mayo Clinic, Stanford, Oxford — repositórios acadêmicos de acesso aberto",
        items = listOf(
            LibraryContentItem(
                id = "ref_032", title = "Harvard DASH — Repositório Institucional de Harvard",
                category = "CLINICA_MEDICA",
                summary = "Teses, artigos e publicações de acesso aberto da Universidade de Harvard.",
                content = "Digital Access to Scholarship at Harvard (DASH) é o repositório institucional de acesso aberto da Harvard University. Contém teses de doutorado, artigos de periódicos, capítulos de livros, e working papers produzidos por pesquisadores de Harvard. Política de acesso aberto obrigatória desde 2008. Cobre todas as áreas, incluindo Harvard Medical School e Harvard School of Public Health.",
                tags = listOf("harvard", "dash", "repositório", "acesso aberto", "teses"),
                citation = "Harvard University", sourceUrl = "https://dash.harvard.edu", sourceRef = "Repositório DASH"
            ),
            LibraryContentItem(
                id = "ref_033", title = "Harvard Health — Artigos de saúde por especialistas",
                category = "CLINICA_MEDICA",
                summary = "Artigos de saúde escritos por especialistas de Harvard Medical School.",
                content = "Harvard Health Publishing é a divisão de comunicação em saúde da Harvard Medical School. Oferece artigos revisados por especialistas sobre uma ampla gama de tópicos médicos: doenças crônicas, saúde preventiva, nutrição, exercício, saúde mental, envelhecimento, e novas pesquisas. Newsletter gratuita 'Harvard Health Letter' com conteúdo baseado em evidências.",
                tags = listOf("harvard", "health", "artigos", "prevenção", "bem-estar"),
                citation = "Harvard Medical School", sourceUrl = "https://www.health.harvard.edu", sourceRef = "Harvard Health Publishing"
            ),
            LibraryContentItem(
                id = "ref_034", title = "Harvard Gazette — Notícias de pesquisa médica",
                category = "CLINICA_MEDICA",
                summary = "Notícias e reportagens sobre pesquisas da Harvard Medical School.",
                content = "O Harvard Gazette é o jornal oficial da Universidade de Harvard. A seção de medicina cobre descobertas de pesquisa, avanços em tratamento, políticas de saúde, e perfil de pesquisadores da Harvard Medical School e hospitais afiliados (Mass General, Brigham and Women's, Beth Israel). Conteúdo gratuito, atualizado diariamente.",
                tags = listOf("harvard", "gazette", "notícias", "pesquisa", "medicina"),
                citation = "Harvard University", sourceUrl = "https://news.harvard.edu", sourceRef = "Seção Medicina Harvard Gazette"
            ),
            LibraryContentItem(
                id = "ref_035", title = "Harvard Medical School — Acesso aberto",
                category = "CLINICA_MEDICA",
                summary = "Política de acesso aberto que torna públicos todos os artigos financiados pelo NIH.",
                content = "A Harvard Medical School tem uma política de acesso aberto que exige que todos os artigos financiados pelo NIH sejam depositados no PubMed Central (PMC) e no DASH. O portal HMS reúne informações sobre as pesquisas e publicações da faculdade, incluindo acesso a periódicos, bibliotecas (Countway Library), e recursos educacionais para profissionais de saúde.",
                tags = listOf("harvard", "hms", "acesso aberto", "pesquisa", "publicações"),
                citation = "Harvard Medical School", sourceUrl = "https://hms.harvard.edu", sourceRef = "Portal HMS"
            ),
            LibraryContentItem(
                id = "ref_036", title = "Welch Medical Library — Johns Hopkins",
                category = "CLINICA_MEDICA",
                summary = "Biblioteca da Johns Hopkins com guias de recursos gratuitos e acesso a bases de dados.",
                content = "A Welch Medical Library é a biblioteca da Johns Hopkins University School of Medicine. Oferece guias de recursos gratuitos por especialidade, acesso a bases de dados (PubMed, Cochrane, Embase, Scopus), tutoriais de pesquisa clínica, e gerenciamento de referências. O 'Welch Research Guides' organiza fontes de informação por tópico médico com curadoria de bibliotecários especializados.",
                tags = listOf("johns hopkins", "welch", "biblioteca", "guias", "recursos"),
                citation = "Johns Hopkins University", sourceUrl = "https://browse.welch.jhmi.edu", sourceRef = "Welch Medical Library Guides"
            ),
            LibraryContentItem(
                id = "ref_037", title = "Hopkins Digital Library — Repositório Johns Hopkins",
                category = "CLINICA_MEDICA",
                summary = "Repositório institucional da Johns Hopkins com artigos e publicações acadêmicas.",
                content = "O Johns Hopkins Digital Library é o repositório institucional da Johns Hopkins University. Preserva e disponibiliza a produção acadêmica da universidade, incluindo artigos de periódicos, teses, dissertações, dados de pesquisa, e materiais de ensino. A Johns Hopkins Medicine é uma das instituições de pesquisa médica mais bem financiadas dos EUA.",
                tags = listOf("johns hopkins", "repositório", "acadêmico", "pesquisa"),
                citation = "Johns Hopkins University", sourceUrl = "https://bob.library.jhu.edu", sourceRef = "JH Digital Library"
            ),
            LibraryContentItem(
                id = "ref_038", title = "Johns Hopkins Medicine — Portal de saúde",
                category = "CLINICA_MEDICA",
                summary = "Publicações, notícias de pesquisa e recursos educacionais da Johns Hopkins Medicine.",
                content = "Portal oficial da Johns Hopkins Medicine, integrando a escola de medicina, hospitais e institutos de pesquisa. Oferece: notícias de descobertas médicas, guias de saúde para pacientes, informações sobre ensaios clínicos, educação médica continuada (CME), e publicações científicas. Conteúdo revisado por especialistas da Johns Hopkins.",
                tags = listOf("johns hopkins", "medicine", "saúde", "pesquisa", "educação"),
                citation = "Johns Hopkins Medicine", sourceUrl = "https://www.hopkinsmedicine.org", sourceRef = "Portal Hopkins Medicine"
            ),
            LibraryContentItem(
                id = "ref_039", title = "Mayo Clinic Proceedings — Periódico científico",
                category = "CLINICA_MEDICA",
                summary = "Periódico científico da Mayo Clinic com artigos de acesso aberto.",
                content = "O Mayo Clinic Proceedings é um periódico médico mensal revisado por pares, com foco em medicina clínica e pesquisa translacional. Artigos de acesso aberto disponíveis online. Cobre: cardiologia, oncologia, neurologia, gastroenterologia, endocrinologia, e medicina interna. Fator de impacto elevado. Indexado no PubMed.",
                tags = listOf("mayo clinic", "proceedings", "periódico", "pesquisa", "acesso aberto"),
                citation = "Mayo Clinic", sourceUrl = "https://mayoclinicproceedings.org", sourceRef = "Mayo Clinic Proceedings"
            ),
            LibraryContentItem(
                id = "ref_040", title = "Mayo Clinic Research — Publicações e recursos",
                category = "CLINICA_MEDICA",
                summary = "Portal de publicações, notícias e recursos educacionais da Mayo Clinic.",
                content = "O portal de pesquisa da Mayo Clinic oferece acesso a: publicações científicas dos pesquisadores da Mayo, informações sobre ensaios clínicos em andamento, centros de pesquisa especializados (câncer, cardiovascular, neurociências, genômica), e oportunidades de colaboração. A Mayo Clinic é uma das maiores instituições de pesquisa clínica integrada do mundo.",
                tags = listOf("mayo clinic", "pesquisa", "publicações", "ensaios clínicos"),
                citation = "Mayo Clinic", sourceUrl = "https://www.mayoclinic.org/research", sourceRef = "Mayo Clinic Research"
            ),
            LibraryContentItem(
                id = "ref_041", title = "Mayo Clinic Health Letter — Newsletter digital",
                category = "CLINICA_MEDICA",
                summary = "Newsletter digital com informações práticas sobre saúde da Mayo Clinic.",
                content = "A Mayo Clinic Health Letter é uma publicação mensal que oferece informações práticas e baseadas em evidências sobre saúde e bem-estar. Tópicos: prevenção de doenças, gerenciamento de condições crônicas, nutrição, exercício, e novas descobertas médicas. Escrita em linguagem acessível para pacientes e profissionais. Versão impressa e digital disponível.",
                tags = listOf("mayo clinic", "health letter", "newsletter", "saúde preventiva"),
                citation = "Mayo Clinic", sourceUrl = "https://order.store.mayoclinic.com", sourceRef = "Mayo Clinic Health Letter"
            ),
            LibraryContentItem(
                id = "ref_042", title = "Stanford Medicine — Portal de pesquisa e educação",
                category = "CLINICA_MEDICA",
                summary = "Publicações e pesquisas da Universidade de Stanford em medicina.",
                content = "Stanford Medicine integra a Stanford University School of Medicine, Stanford Health Care e hospitais afiliados. O portal oferece: notícias de pesquisa, publicações científicas, educação médica, informações para pacientes, e recursos de inovação em saúde. Stanford é líder em pesquisa genômica, inteligência artificial em medicina, e medicina regenerativa.",
                tags = listOf("stanford", "medicine", "pesquisa", "inovação", "educação"),
                citation = "Stanford University", sourceUrl = "https://med.stanford.edu", sourceRef = "Portal Stanford Medicine"
            ),
            LibraryContentItem(
                id = "ref_043", title = "UCSF Library — Acesso aberto em saúde",
                category = "CLINICA_MEDICA",
                summary = "Recursos de acesso aberto da Universidade da Califórnia em São Francisco.",
                content = "A UCSF Library é a biblioteca da University of California, San Francisco, instituição dedicada exclusivamente às ciências da saúde. Oferece: guias de pesquisa por especialidade, repositório institucional (UCSF Scholarship), acesso a bases de dados (PubMed, Embase, Web of Science), e recursos de dados abertos em saúde. UCSF é líder em pesquisa clínica e biomédica.",
                tags = listOf("ucsf", "biblioteca", "acesso aberto", "pesquisa", "guias"),
                citation = "University of California, San Francisco", sourceUrl = "https://library.ucsf.edu", sourceRef = "UCSF Library"
            ),
            LibraryContentItem(
                id = "ref_044", title = "Oxford Academic — Periódicos de Oxford",
                category = "CLINICA_MEDICA",
                summary = "Periódicos de acesso aberto da Universidade de Oxford.",
                content = "Oxford Academic é a plataforma de publicações acadêmicas da Oxford University Press (OUP). Oferece centenas de periódicos revisados por pares, incluindo títulos de acesso aberto em medicina: European Heart Journal, Journal of Infectious Diseases, Clinical Infectious Diseases, e Nephrology Dialysis Transplantation. Artigos de acesso aberto identificados com selo 'Open Access'.",
                tags = listOf("oxford", "academic", "periódicos", "acesso aberto", "oup"),
                citation = "Oxford University Press", sourceUrl = "https://academic.oup.com", sourceRef = "Oxford Academic"
            ),
            LibraryContentItem(
                id = "ref_045", title = "KI Open Archive — Karolinska Institute",
                category = "CLINICA_MEDICA",
                summary = "Publicações científicas do Karolinska Institute, instituição do Prêmio Nobel de Medicina.",
                content = "O KI Open Archive (OpenArchive.ki.se) é o repositório institucional do Karolinska Institute (Suécia), uma das principais universidades médicas do mundo e responsável pela seleção do Prêmio Nobel de Fisiologia ou Medicina. Contém teses, artigos e publicações científicas em acesso aberto. Foco em: neurociências, câncer, epidemiologia, e medicina translacional.",
                tags = listOf("karolinska", "repositório", "nobel", "pesquisa", "acesso aberto"),
                citation = "Karolinska Institute", sourceUrl = "https://openarchive.ki.se", sourceRef = "KI Open Archive"
            ),
            LibraryContentItem(
                id = "ref_046", title = "Washington University Open Access",
                category = "CLINICA_MEDICA",
                summary = "Artigos revisados por pares da Washington University em St. Louis.",
                content = "O Washington University Open Access (Digital Commons) é o repositório institucional da Washington University em St. Louis. Engloba a School of Medicine (WashU Med), que é referência em: cardiologia, oncologia (Siteman Cancer Center), genética, neurociências, e oftalmologia. Artigos, teses e dados de pesquisa disponíveis gratuitamente.",
                tags = listOf("washington university", "repositório", "acesso aberto", "st louis"),
                citation = "Washington University in St. Louis", sourceUrl = "https://digitalcommons.wustl.edu", sourceRef = "Washington University Open Access"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 5: PERIÓDICOS DE ACESSO ABERTO (47-53)
    // ═══════════════════════════════════════════════════════════════

    private val openAccessJournalsPack = LibraryContentPack(
        source = "BioMed Central, PLOS Medicine, BMJ Open, CDC — periódicos de acesso aberto",
        items = listOf(
            LibraryContentItem(
                id = "ref_047", title = "BioMed Central — Periódicos de acesso aberto",
                category = "CLINICA_MEDICA",
                summary = "Editora com centenas de periódicos de acesso aberto em todas as áreas biomédicas.",
                content = "BioMed Central (BMC) é uma editora pioneira em acesso aberto no campo biomédico, parte da Springer Nature. Publica centenas de periódicos revisados por pares, incluindo: BMC Medicine, BMC Public Health, BMC Infectious Diseases, BMC Cardiovascular Disorders, e BMC Complementary and Alternative Medicine. Todos os artigos são imediatamente de acesso aberto sob licença Creative Commons.",
                tags = listOf("biomed central", "bmc", "acesso aberto", "periódicos", "biomedicina"),
                citation = "BioMed Central (Springer Nature)", sourceUrl = "https://www.biomedcentral.com", sourceRef = "Portal BMC"
            ),
            LibraryContentItem(
                id = "ref_048", title = "PLOS Medicine — Medicina de acesso aberto",
                category = "CLINICA_MEDICA",
                summary = "Periódico de acesso aberto com artigos de alto impacto em medicina clínica.",
                content = "PLOS Medicine é um periódico de acesso aberto revisado por pares, focado em artigos de alto impacto em medicina clínica, saúde global e políticas de saúde. Publica: ensaios clínicos randomizados, revisões sistemáticas, estudos de coorte, e análises de políticas de saúde. Fator de impacto elevado. Licenciamento CC-BY permite reuso e distribuição.",
                tags = listOf("plos medicine", "acesso aberto", "alto impacto", "medicina clínica"),
                citation = "Public Library of Science", sourceUrl = "https://journals.plos.org/plosmedicine", sourceRef = "PLOS Medicine"
            ),
            LibraryContentItem(
                id = "ref_049", title = "BMJ Open — Acesso aberto do BMJ",
                category = "CLINICA_MEDICA",
                summary = "Periódico de acesso aberto do British Medical Journal.",
                content = "BMJ Open é um periódico de acesso aberto do British Medical Journal (BMJ). Publica artigos de pesquisa originais em todas as especialidades médicas e áreas relacionadas à saúde. Diferencia-se por publicar o protocolo do estudo junto com os resultados (transparência metodológica). Taxas de publicação reduzidas para autores de países de baixa e média renda.",
                tags = listOf("bmj open", "acesso aberto", "bmj", "pesquisa original"),
                citation = "British Medical Journal", sourceUrl = "https://bmjopen.bmj.com", sourceRef = "BMJ Open"
            ),
            LibraryContentItem(
                id = "ref_050", title = "Pan American Journal of Public Health — OPAS",
                category = "CLINICA_MEDICA",
                summary = "Periódico da OPAS, totalmente gratuito para autores e leitores.",
                content = "O Pan American Journal of Public Health (Revista Panamericana de Salud Pública) é o periódico oficial da Organização Pan-Americana da Saúde (OPAS/OMS). Totalmente gratuito para autores e leitores. Publica artigos originais em saúde pública nas Américas. Idiomas: português, espanhol e inglês. Indexado no PubMed e SciELO.",
                tags = listOf("opas", "saúde pública", "periódico", "américas", "gratuito"),
                citation = "Pan American Health Organization", sourceUrl = "https://www.paho.org/journal", sourceRef = "PAHO Journal"
            ),
            LibraryContentItem(
                id = "ref_051", title = "MMWR (CDC) — Relatório semanal de morbidade e mortalidade",
                category = "CLINICA_MEDICA",
                summary = "Boletim epidemiológico semanal do CDC, acesso aberto.",
                content = "O MMWR (Morbidity and Mortality Weekly Report) é o boletim epidemiológico semanal do CDC. Publica: notificações de surtos, dados de vigilância de doenças, recomendações de saúde pública (ACIP), e relatórios de investigação de casos. Essencial para profissionais de saúde acompanharem eventos de saúde pública em tempo real. Acesso aberto imediato.",
                tags = listOf("mmwr", "cdc", "epidemiologia", "vigilância", "boletim"),
                citation = "Centers for Disease Control and Prevention", sourceUrl = "https://stacks.cdc.gov/mmwr", sourceRef = "MMWR no CDC Stacks"
            ),
            LibraryContentItem(
                id = "ref_052", title = "Emerging Infectious Diseases (CDC) — Doenças emergentes",
                category = "CLINICA_MEDICA",
                summary = "Periódico de acesso aberto do CDC sobre doenças infecciosas emergentes.",
                content = "Emerging Infectious Diseases (EID) é um periódico mensal de acesso aberto do CDC, focado em vigilância e pesquisa de doenças infecciosas emergentes e reemergentes. Publica: artigos originais, relatos de casos, revisões, e dispatches epidemiológicos. Indexado no PubMed, Scopus e Web of Science. Fator de impacto elevado.",
                tags = listOf("eid", "cdc", "doenças infecciosas", "emergentes", "periódico"),
                citation = "Centers for Disease Control and Prevention", sourceUrl = "https://wwwnc.cdc.gov/eid", sourceRef = "EID Journal"
            ),
            LibraryContentItem(
                id = "ref_053", title = "Free Medical Journals — Diretório de periódicos gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Diretório com mais de 2.000 periódicos científicos com acesso gratuito ao texto completo.",
                content = "Free Medical Journals é um diretório que cataloga periódicos científicos com acesso gratuito ao texto completo. Mais de 2.000 periódicos organizados por especialidade médica e por idioma. Inclui desde periódicos completamente abertos até aqueles com conteúdo gratuito seletivo. Atualizado regularmente para garantir que os links permaneçam ativos.",
                tags = listOf("free medical journals", "periódicos", "gratuito", "diretório", "texto completo"),
                citation = "Free Medical Journals", sourceUrl = "https://www.freemedicaljournals.com", sourceRef = "Free Medical Journals"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 6: RECURSOS ADICIONAIS (54-60)
    // ═══════════════════════════════════════════════════════════════

    private val additionalResourcesPack = LibraryContentPack(
        source = "AccessMedicine, MDCalc, MedPix, Radiopaedia, Epocrates — ferramentas clínicas digitais",
        items = listOf(
            LibraryContentItem(
                id = "ref_054", title = "AccessMedicine — Biblioteca digital médica",
                category = "CLINICA_MEDICA",
                summary = "Biblioteca digital com textos completos de Harrison's, CMDT e outros clássicos.",
                content = "AccessMedicine é uma plataforma da McGraw Hill que oferece acesso a livros-texto fundamentais da medicina: Harrison's Principles of Internal Medicine, Current Medical Diagnosis & Treatment (CMDT), Tintinalli's Emergency Medicine, Lange Clinical Reviews, e Fitzpatrick's Dermatology. Inclui vídeos de procedimentos, calculadoras médicas, casos clínicos interativos, e perguntas para estudo (board review). Acesso geralmente institucional.",
                tags = listOf("accessmedicine", "harrison", "cmdt", "livros", "referência"),
                citation = "McGraw Hill", sourceUrl = "https://accessmedicine.mhmedical.com", sourceRef = "AccessMedicine"
            ),
            LibraryContentItem(
                id = "ref_055", title = "MDCalc — Calculadoras médicas",
                category = "CLINICA_MEDICA",
                summary = "Mais de 900 ferramentas de decisão clínica baseadas em evidências.",
                content = "MDCalc é a plataforma de referência para calculadoras médicas e escores clínicos. Mais de 900 ferramentas de decisão clínica, incluindo: critérios diagnósticos (Duke, Wells, CURB-65), estratificação de risco (CHA₂DS₂-VASc, HAS-BLED, TIMI), calculadoras de dose (creatinina, GFR), e escores prognósticos (APACHE, SOFA, MELD). Cada ferramenta tem referência bibliográfica com nível de evidência e citação do estudo original.",
                tags = listOf("mdcalc", "calculadora", "escore", "decisão clínica", "ferramenta"),
                citation = "MDCalc", sourceUrl = "https://www.mdcalc.com", sourceRef = "Portal MDCalc"
            ),
            LibraryContentItem(
                id = "ref_056", title = "MedPix — Banco de imagens médicas (NLM)",
                category = "CLINICA_MEDICA",
                summary = "Banco de dados gratuito com mais de 59.000 imagens médicas e 12.000 casos clínicos.",
                content = "MedPix é um banco de dados gratuito da National Library of Medicine (NLM) com mais de 59.000 imagens médicas e 12.000 casos clínicos. Cobre todas as especialidades: radiologia, patologia, dermatologia, oftalmologia e cirurgia. Cadastro gratuito necessário. Recurso educacional para ensino médico com casos organizados por diagnóstico, anatomia e modalidade de imagem.",
                tags = listOf("medpix", "imagens médicas", "casos clínicos", "nlm", "educação"),
                citation = "National Library of Medicine", sourceUrl = "https://medpix.nlm.nih.gov", sourceRef = "MedPix NLM"
            ),
            LibraryContentItem(
                id = "ref_057", title = "Radiopaedia — Enciclopédia de radiologia",
                category = "CLINICA_MEDICA",
                summary = "Recurso de referência em radiologia com mais de 60.000 casos e 16.000 artigos.",
                content = "Radiopaedia é uma enciclopédia colaborativa e gratuita de radiologia. Mais de 60.000 casos clínicos com imagens e 16.000 artigos revisados por pares. Cobre: radiografia, tomografia, ressonância magnética, ultrassom e medicina nuclear. Organizado por anatomia, patologia e modalidade. Referência essencial para residentes e radiologistas. Aplicativo móvel disponível.",
                tags = listOf("radiopaedia", "radiologia", "imagens", "casos", "referência"),
                citation = "Radiopaedia.org", sourceUrl = "https://radiopaedia.org", sourceRef = "Radiopaedia"
            ),
            LibraryContentItem(
                id = "ref_058", title = "STATdx — Diagnóstico em radiologia (Elsevier)",
                category = "CLINICA_MEDICA",
                summary = "Ferramenta de suporte à decisão diagnóstica em radiologia.",
                content = "STATdx é uma ferramenta de suporte à decisão diagnóstica em radiologia desenvolvida pela Elsevier (Amirsys). Oferece: mais de 4.500 diagnósticos radiológicos organizados por anatomia e modalidade, imagens de alta qualidade com anotações, diagnósticos diferenciais categorizados, e referências bibliográficas. Acesso geralmente institucional via assinatura.",
                tags = listOf("statdx", "radiologia", "diagnóstico", "elsevier", "imagens"),
                citation = "Elsevier", sourceUrl = "https://www.elsevier.com/clinical-solutions/statdx", sourceRef = "STATdx"
            ),
            LibraryContentItem(
                id = "ref_059", title = "Epocrates — Referência farmacológica móvel",
                category = "CLINICA_MEDICA",
                summary = "Aplicativo de referência clínica com informações sobre medicamentos e interações.",
                content = "Epocrates é um aplicativo de referência clínica amplamente utilizado por médicos. Versão gratuita inclui: monografias de medicamentos (doses, indicações, contraindicações), verificador de interações medicamentosas, identificador de pílulas por cor/forma/impressão, e calculadoras médicas. A versão paga adiciona: diretrizes, tabelas de doses pediátricas, e acesso a conteúdo de especialidades.",
                tags = listOf("epocrates", "farmacologia", "interações", "medicamentos", "app"),
                citation = "Epocrates (Athenahealth)", sourceUrl = "https://www.epocrates.com", sourceRef = "Epocrates"
            ),
            LibraryContentItem(
                id = "ref_060", title = "IBM Micromedex — Base farmacológica",
                category = "CLINICA_MEDICA",
                summary = "Base de dados farmacológica com informações sobre medicamentos, interações e toxicologia.",
                content = "O IBM Micromedex é uma base de dados farmacológica abrangente, considerado padrão ouro para informações sobre medicamentos em ambiente hospitalar. Inclui: monografias de drogas, doses pediátricas e adultas, interações medicamentosas, ajustes na insuficiência renal/hepática, toxicologia, e avaliação de segurança na gestação e lactação. Conteúdo revisado por farmacêuticos clínicos. Acesso institucional.",
                tags = listOf("micromedex", "ibm", "farmacologia", "interações", "toxicologia"),
                citation = "IBM Watson Health", sourceUrl = "https://www.ibm.com/products/micromedex", sourceRef = "IBM Micromedex"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 7: MINISTÉRIO DA SAÚDE BRASIL (61-66)
    // ═══════════════════════════════════════════════════════════════

    private val msBrasilPack = LibraryContentPack(
        source = "Ministério da Saúde do Brasil — PCDT, RENAME, Cadernos de Atenção Básica, REBRATS",
        items = listOf(
            LibraryContentItem(
                id = "ref_061", title = "PCDT — Protocolos Clínicos e Diretrizes Terapêuticas",
                category = "CLINICA_MEDICA",
                summary = "Protocolos oficiais do SUS que estabelecem critérios de diagnóstico e tratamento.",
                content = "Os Protocolos Clínicos e Diretrizes Terapêuticas (PCDT) são documentos técnico-científicos elaborados pelo Ministério da Saúde do Brasil que estabelecem: critérios de diagnóstico, algoritmo de tratamento, medicamentos e insumos disponíveis no SUS, e monitoramento clínico para doenças específicas. Base legal para incorporação de tecnologias no SUS. Mais de 100 PCDTs publicados, abrangendo oncologia, cardiologia, neurologia, reumatologia, doenças infecciosas e raras.",
                tags = listOf("ms brasil", "pcdt", "protocolos", "sus", "diretrizes"),
                citation = "Ministério da Saúde do Brasil", sourceUrl = "https://www.gov.br/saude/pt-br/assuntos/pcdt", sourceRef = "Seção PCDT"
            ),
            LibraryContentItem(
                id = "ref_062", title = "RENAME — Relação Nacional de Medicamentos Essenciais",
                category = "CLINICA_MEDICA",
                summary = "Lista oficial de medicamentos disponíveis no SUS.",
                content = "A Relação Nacional de Medicamentos Essenciais (RENAME) é a lista oficial de medicamentos e insumos disponibilizados pelo SUS. Atualizada periodicamente pela Comissão Nacional de Incorporação de Tecnologias no SUS (CONITEC). A RENAME 2022 inclui mais de 800 medicamentos. Organiza-se por componentes: básico (atenção primária), estratégico (HIV, tuberculose, hanseníase), especializado (alta complexidade), e hospitalar.",
                tags = listOf("rename", "medicamentos", "sus", "lista", "essenciais"),
                citation = "Ministério da Saúde do Brasil", sourceUrl = "https://www.gov.br/saude/pt-br/assuntos/rename", sourceRef = "Seção RENAME"
            ),
            LibraryContentItem(
                id = "ref_063", title = "Guia de Vigilância Epidemiológica — MS",
                category = "CLINICA_MEDICA",
                summary = "Referência para notificação, prevenção e controle de doenças de notificação compulsória.",
                content = "O Guia de Vigilância Epidemiológica é um documento de referência do Ministério da Saúde para profissionais de saúde. Abrange: lista nacional de doenças de notificação compulsória, definições de caso, fluxos de notificação (SINAN), medidas de prevenção e controle, e recomendações para investigação de surtos. Essencial para médicos que atuam em serviços de urgência, atenção primária e epidemiologia hospitalar.",
                tags = listOf("vigilância epidemiológica", "notificação", "ms brasil", "sinan"),
                citation = "Ministério da Saúde do Brasil", sourceUrl = "https://www.gov.br/saude/pt-br/assuntos/saude-de-a-a-z", sourceRef = "Seção Vigilância Epidemiológica"
            ),
            LibraryContentItem(
                id = "ref_064", title = "Cadernos de Atenção Básica (CAB) — APS",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes oficiais para a Atenção Primária à Saúde e Estratégia Saúde da Família.",
                content = "Os Cadernos de Atenção Básica (CAB) são publicações do Ministério da Saúde que estabelecem diretrizes para o cuidado na Atenção Primária à Saúde (APS) e Estratégia Saúde da Família (ESF). Mais de 40 cadernos publicados cobrindo: hipertensão, diabetes, saúde da criança, saúde da mulher, saúde mental, tuberculose, hanseníase, e procedimentos básicos. Base para o processo de trabalho das equipes de ESF em todo o Brasil.",
                tags = listOf("atenção básica", "aps", "esf", "cadernos", "diretrizes"),
                citation = "Ministério da Saúde do Brasil", sourceUrl = "https://www.gov.br/saude/pt-br/assuntos/saude-de-a-a-z", sourceRef = "Cadernos de Atenção Básica"
            ),
            LibraryContentItem(
                id = "ref_065", title = "Saúde de A a Z — Guia do MS",
                category = "CLINICA_MEDICA",
                summary = "Guia com informações oficiais sobre sintomas, diagnósticos, tratamentos e prevenção.",
                content = "O portal 'Saúde de A a Z' do Ministério da Saúde do Brasil é um guia abrangente com informações técnicas sobre: doenças e agravos (descrição, sintomas, tratamento, prevenção), programas de saúde (vacinação, pré-natal, DST/Aids, tabagismo), e serviços do SUS. Conteúdo organizado alfabeticamente por tema, voltado para profissionais de saúde e gestores.",
                tags = listOf("saúde de a a z", "ms brasil", "guias", "doenças", "prevenção"),
                citation = "Ministério da Saúde do Brasil", sourceUrl = "https://www.gov.br/saude/pt-br/assuntos/saude-de-a-a-z", sourceRef = "Portal Saúde de A a Z"
            ),
            LibraryContentItem(
                id = "ref_066", title = "REBRATS — Rede Brasileira de Evidências em Saúde",
                category = "CLINICA_MEDICA",
                summary = "Portal com diretrizes clínicas, protocolos e avaliações de tecnologias em saúde.",
                content = "A Rede Brasileira de Evidências em Saúde (REBRATS) é uma plataforma do Ministério da Saúde que reúne: diretrizes clínicas baseadas em evidências, protocolos de saúde, avaliações de tecnologias em saúde (ATS), revisões sistemáticas, e sumários de evidências. Colaboração entre MS, CONITEC, universidades e sociedades médicas. Ferramenta essencial para prática baseada em evidências no Brasil.",
                tags = listOf("rebrats", "evidências", "diretrizes", "ats", "ms brasil"),
                citation = "Ministério da Saúde do Brasil", sourceUrl = "https://rebrats.saude.gov.br", sourceRef = "Portal REBRATS"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 8: ANVISA (67-69)
    // ═══════════════════════════════════════════════════════════════

    private val anvisaPack = LibraryContentPack(
        source = "ANVISA — Bulário Eletrônico, Registro de Medicamentos, Farmacovigilância",
        items = listOf(
            LibraryContentItem(
                id = "ref_067", title = "Bulário Eletrônico da ANVISA",
                category = "CLINICA_MEDICA",
                summary = "Base de dados com bulas completas de mais de 7.000 medicamentos registrados no Brasil.",
                content = "O Bulário Eletrônico da ANVISA é a base de dados oficial com bulas completas de mais de 7.000 medicamentos registrados no Brasil. Cada bula contém: identificação do medicamento, forma farmacêutica, indicações, contraindicações, posologia, reações adversas, interações medicamentosas, e cuidados de armazenamento. Atualizado conforme novas aprovações e alterações de registro. Fonte oficial e legal para consulta de prescrição no Brasil.",
                tags = listOf("anvisa", "bulário", "medicamentos", "bulas", "brasil"),
                citation = "ANVISA", sourceUrl = "https://www.gov.br/anvisa/pt-br/assuntos/medicamentos/bulario-eletronico", sourceRef = "Bulário Eletrônico ANVISA"
            ),
            LibraryContentItem(
                id = "ref_068", title = "ANVISA — Consulta de Registro de Medicamentos",
                category = "CLINICA_MEDICA",
                summary = "Sistema para verificar se um medicamento possui registro sanitário válido na ANVISA.",
                content = "O sistema de Consultas de Medicamentos da ANVISA permite verificar a situação do registro sanitário de medicamentos no Brasil. Informações disponíveis: número do registro, validade, detentor do registro, apresentações comercializadas, e situação (válido, cancelado, suspenso). Essencial para verificar a regularidade de medicamentos antes da prescrição ou compra institucional.",
                tags = listOf("anvisa", "registro", "consulta", "medicamentos", "regularização"),
                citation = "ANVISA", sourceUrl = "https://consultas.anvisa.gov.br", sourceRef = "Consultas ANVISA"
            ),
            LibraryContentItem(
                id = "ref_069", title = "Farmacovigilância (Notivisa) — ANVISA",
                category = "CLINICA_MEDICA",
                summary = "Sistema de notificação de eventos adversos e queixas técnicas de medicamentos.",
                content = "O sistema de Farmacovigilância da ANVISA (Notivisa) é a plataforma oficial para notificação de eventos adversos, queixas técnicas e desvios de qualidade relacionados a medicamentos, vacinas, saneantes e outros produtos de saúde. Profissionais de saúde podem notificar anonimamente. Essencial para o monitoramento da segurança de medicamentos pós-comercialização no Brasil. A vigilância ativa da ANVISA contribui para recalls e alertas de segurança.",
                tags = listOf("anvisa", "farmacovigilância", "notivisa", "eventos adversos", "segurança"),
                citation = "ANVISA", sourceUrl = "https://www.gov.br/anvisa/pt-br/assuntos/farmacovigilancia", sourceRef = "Farmacovigilância ANVISA"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 9: FARMÁCIA E PRESCRIÇÃO (70-73)
    // ═══════════════════════════════════════════════════════════════

    private val pharmacyPack = LibraryContentPack(
        source = "Drugs.com, Micromedex, Lexicomp, Epocrates — referências farmacológicas internacionais",
        items = listOf(
            LibraryContentItem(
                id = "ref_070", title = "Drugs.com — Base de dados internacional de medicamentos",
                category = "CLINICA_MEDICA",
                summary = "Informações completas sobre medicamentos, interações e efeitos colaterais.",
                content = "Drugs.com é uma base de dados farmacológica internacional gratuita (financiada por publicidade). Oferece: monografias de mais de 24.000 medicamentos, verificador de interações medicamentosas (drug interactions checker), calculadora de doses, informações sobre efeitos colaterais, guias de medicação para pacientes, e notícias regulatórias (FDA alertas). Conteúdo revisado por farmacêuticos e médicos. Aplicativo móvel disponível.",
                tags = listOf("drugs.com", "medicamentos", "interações", "farmacologia", "gratuito"),
                citation = "Drugs.com", sourceUrl = "https://www.drugs.com", sourceRef = "Portal Drugs.com"
            ),
            LibraryContentItem(
                id = "ref_071", title = "Micromedex (IBM) — Base farmacológica",
                category = "CLINICA_MEDICA",
                summary = "Base de dados farmacológica para consulta de doses, interações e toxicologia.",
                content = "O IBM Micromedex é referência em informação farmacológica para hospitais e farmácias clínicas. Oferece: avaliação de interações medicamentosas (DrugDex), toxicologia (Poindex), doses pediátricas (NeoFax), monografias com evidências classificadas (classe I, IIa, IIb), e conteúdo sobre aleitamento e gestação. Conteúdo revisado e atualizado continuamente. Acesso institucional.",
                tags = listOf("micromedex", "farmácia", "toxicologia", "interações", "doses"),
                citation = "IBM Watson Health", sourceUrl = "https://www.ibm.com/products/micromedex", sourceRef = "IBM Micromedex"
            ),
            LibraryContentItem(
                id = "ref_072", title = "Lexicomp (UpToDate Lexidrug) — Referência farmacológica",
                category = "CLINICA_MEDICA",
                summary = "Referência farmacológica para doses pediátricas e adultas, interações e ajustes renais.",
                content = "Lexicomp (agora UpToDate Lexidrug) é uma referência farmacológica da Wolters Kluwer amplamente utilizada em hospitais. Oferece: monografias de medicamentos com doses pediátricas e adultas, interações medicamentosas (Lexi-Interact), ajustes na insuficiência renal e hepática, informações sobre aleitamento e gestação, e identificação de pílulas. Conteúdo integrado com UpToDate para acesso contextual durante a tomada de decisão clínica.",
                tags = listOf("lexicomp", "uptodate", "farmacologia", "doses", "pediatria"),
                citation = "Wolters Kluwer", sourceUrl = "https://www.wolterskluwer.com/en/solutions/lexicomp", sourceRef = "Lexicomp"
            ),
            LibraryContentItem(
                id = "ref_073", title = "Epocrates — Referência móvel de medicamentos",
                category = "CLINICA_MEDICA",
                summary = "Aplicativo de referência clínica com informações sobre medicamentos e interações.",
                content = "Epocrates é um aplicativo de referência clínica para dispositivos móveis, amplamente utilizado por médicos nos EUA e internacionalmente. Oferece: monografias de medicamentos com doses, contraindicações e reações adversas; verificador de interações medicamentosas (multidroga); identificador de pílulas por cor, forma e impressão; e calculadoras médicas integradas. Versão gratuita disponível com funcionalidades básicas.",
                tags = listOf("epocrates", "medicamentos", "app", "interações", "farmacologia"),
                citation = "Epocrates (Athenahealth)", sourceUrl = "https://www.epocrates.com", sourceRef = "Epocrates"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 10: SOCIEDADES MÉDICAS BRASILEIRAS (74-79)
    // ═══════════════════════════════════════════════════════════════

    private val brazilianSocietiesPack = LibraryContentPack(
        source = "SBC, SBPT, SBEM, FEBRASGO, SBR, SBI — sociedades médicas brasileiras",
        items = listOf(
            LibraryContentItem(
                id = "ref_074", title = "SBC — Sociedade Brasileira de Cardiologia",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes e posicionamentos oficiais da SBC para cardiologia.",
                content = "A Sociedade Brasileira de Cardiologia (SBC) é a entidade máxima da cardiologia no Brasil. Publica: diretrizes oficiais (hipertensão, insuficiência cardíaca, IAM, fibrilação atrial, dislipidemias), posicionamentos, consensos, e o periódico Arquivos Brasileiros de Cardiologia (ABC Cardiol). As diretrizes da SBC são referência obrigatória para a prática cardiológica no Brasil, alinhadas com evidências internacionais adaptadas ao contexto nacional.",
                tags = listOf("sbc", "cardiologia", "diretrizes", "cardiol", "brasil"),
                citation = "Sociedade Brasileira de Cardiologia", sourceUrl = "https://www.cardiol.br", sourceRef = "Portal SBC"
            ),
            LibraryContentItem(
                id = "ref_075", title = "SBPT — Sociedade Brasileira de Pneumologia e Tisiologia",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes para asma, DPOC, tuberculose, e doenças respiratórias.",
                content = "A Sociedade Brasileira de Pneumologia e Tisiologia (SBPT) publica diretrizes e consensos nacionais para: asma, DPOC, tuberculose, doenças pulmonares intersticiais, hipertensão pulmonar, tromboembolismo pulmonar, ventilação mecânica, e doenças do sono. Também edita o Jornal Brasileiro de Pneumologia (JBPneu). Foco principal: tuberculose (Brasil é um dos 30 países de alta carga da doença) e tabagismo.",
                tags = listOf("sbpt", "pneumologia", "tisiologia", "asma", "dpoc"),
                citation = "Sociedade Brasileira de Pneumologia e Tisiologia", sourceUrl = "https://www.sbpt.org.br", sourceRef = "Portal SBPT"
            ),
            LibraryContentItem(
                id = "ref_076", title = "SBEM — Sociedade Brasileira de Endocrinologia e Metabologia",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes para diabetes, tireoide, dislipidemia, osteoporose e outras doenças endócrinas.",
                content = "A Sociedade Brasileira de Endocrinologia e Metabologia (SBEM) é a entidade de referência em endocrinologia no Brasil. Publica: diretrizes oficiais (diabetes mellitus, tireoide, obesidade, osteoporose, dislipidemia, síndrome metabólica), posicionamentos, e consensos nacionais. Edita os Arquivos Brasileiros de Endocrinologia & Metabologia (ABE&M). Representante brasileiro na International Society of Endocrinology (ISE) e na American Thyroid Association (ATA).",
                tags = listOf("sbem", "endocrinologia", "diabetes", "tireoide", "diretrizes"),
                citation = "Sociedade Brasileira de Endocrinologia e Metabologia", sourceUrl = "https://www.endocrino.org.br", sourceRef = "Portal SBEM"
            ),
            LibraryContentItem(
                id = "ref_077", title = "FEBRASGO — Federação Brasileira de Ginecologia e Obstetrícia",
                category = "CLINICA_MEDICA",
                summary = "Protocolos e diretrizes em obstetrícia e ginecologia.",
                content = "A Federação Brasileira de Ginecologia e Obstetrícia (FEBRASGO) é a entidade máxima da ginecologia e obstetrícia no Brasil. Publica: protocolos assistenciais (pré-natal, parto, puerpério, doenças ginecológicas), manuais de orientação (anticoncepção, climatério, endometriose, HPV), e as séries 'Protocolos FEBRASGO' e 'Tratado de Obstetrícia'. Edita a Revista Femina, de acesso aberto.",
                tags = listOf("febrasgo", "ginecologia", "obstetrícia", "protocolos", "materno-infantil"),
                citation = "Federação Brasileira de Ginecologia e Obstetrícia", sourceUrl = "https://www.febrasgo.org.br", sourceRef = "Portal FEBRASGO"
            ),
            LibraryContentItem(
                id = "ref_078", title = "SBR — Sociedade Brasileira de Reumatologia",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes e protocolos em doenças reumáticas.",
                content = "A Sociedade Brasileira de Reumatologia (SBR) publica: diretrizes oficiais (artrite reumatoide, lúpus eritematoso sistêmico, osteoartrite, gota, fibromialgia, espondilite anquilosante), posicionamentos sobre uso de medicamentos biológicos e imunossupressores, consensos nacionais, e o periódico Revista Brasileira de Reumatologia (RBR). Referência para prática reumatológica no Brasil.",
                tags = listOf("sbr", "reumatologia", "diretrizes", "artrite", "lúpus"),
                citation = "Sociedade Brasileira de Reumatologia", sourceUrl = "https://www.reumatologia.org.br", sourceRef = "Portal SBR"
            ),
            LibraryContentItem(
                id = "ref_079", title = "SBI — Sociedade Brasileira de Infectologia",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes e posicionamentos em doenças infecciosas.",
                content = "A Sociedade Brasileira de Infectologia (SBI) publica: diretrizes e consensos (HIV/Aids, hepatites virais, tuberculose, infecções sexualmente transmissíveis, antibioticoterapia, infecções hospitalares), posicionamentos sobre surtos e emergências (dengue, chikungunya, zika, COVID-19), e o periódico Brazilian Journal of Infectious Diseases (BJID). Referência para controle de infecção e uso racional de antimicrobianos no Brasil.",
                tags = listOf("sbi", "infectologia", "diretrizes", "hiv", "antimicrobianos"),
                citation = "Sociedade Brasileira de Infectologia", sourceUrl = "https://www.sbimedicina.com.br", sourceRef = "Portal SBI"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 11: DIRETRIZES CLÍNICAS INTERNACIONAIS (80-88)
    // ═══════════════════════════════════════════════════════════════

    private val guidelinesPack = LibraryContentPack(
        source = "NICE, AHRQ, GIN, ESC, AHA, ESMO, USPSTF, KDIGO — diretrizes clínicas internacionais",
        items = listOf(
            LibraryContentItem(
                id = "ref_080", title = "NICE — National Institute for Health and Care Excellence (Reino Unido)",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes clínicas baseadas em evidências do Reino Unido.",
                content = "O NICE (National Institute for Health and Care Excellence) é a referência mundial em diretrizes clínicas baseadas em evidências. Publica: guidelines clínicos (mais de 300 cobrindo todas as especialidades), guidance de tecnologias em saúde (avaliação de custo-efetividade), padrões de qualidade (quality standards), e indicadores de desempenho. Considerado padrão ouro internacional para a elaboração de diretrizes clínicas.",
                tags = listOf("nice", "diretrizes", "reino unido", "evidência", "guidelines"),
                citation = "National Institute for Health and Care Excellence", sourceUrl = "https://www.nice.org.uk", sourceRef = "Portal NICE"
            ),
            LibraryContentItem(
                id = "ref_081", title = "AHRQ — Agency for Healthcare Research and Quality (EUA)",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes e recomendações baseadas em evidências dos EUA.",
                content = "A Agency for Healthcare Research and Quality (AHRQ) é a agência do governo dos EUA focada em segurança do paciente e qualidade do cuidado. Publica: diretrizes clínicas (National Guideline Clearinghouse — descontinuado, mas migrado para o ECRI Guidelines Trust), evidências para tomada de decisão (Effective Health Care Program), indicadores de qualidade (AHRQ QIs), e ferramentas de segurança do paciente (TeamSTEPPS, CUSP).",
                tags = listOf("ahrq", "qualidade", "segurança do paciente", "diretrizes", "eua"),
                citation = "Agency for Healthcare Research and Quality", sourceUrl = "https://www.ahrq.gov", sourceRef = "Portal AHRQ"
            ),
            LibraryContentItem(
                id = "ref_082", title = "GIN — Guidelines International Network",
                category = "CLINICA_MEDICA",
                summary = "Rede internacional de diretrizes clínicas com biblioteca global.",
                content = "A Guidelines International Network (GIN) é uma rede internacional que reúne organizações desenvolvedoras de diretrizes clínicas de todo o mundo. A GIN International Guidelines Library é um banco de dados pesquisável de diretrizes de diversos países. A GIN também publica padrões metodológicos (GIN Standards) e promove a implementação de diretrizes baseadas em evidências globalmente.",
                tags = listOf("gin", "diretrizes", "internacional", "guidelines", "rede"),
                citation = "Guidelines International Network", sourceUrl = "https://g-i-n.net", sourceRef = "Portal GIN"
            ),
            LibraryContentItem(
                id = "ref_083", title = "ESC — European Society of Cardiology",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes cardiovasculares europeias de alto rigor metodológico.",
                content = "A European Society of Cardiology (ESC) publica diretrizes cardiovasculares consideradas referência mundial. Abrangem: síndromes coronarianas agudas, insuficiência cardíaca, fibrilação atrial, hipertensão, dislipidemias, doença valvar, e cardiologia preventiva. Cada diretriz é desenvolvida por task forces com representação de múltiplos países e publicada no European Heart Journal. Atualizações periódicas com classes de recomendação (I, IIa, IIb, III) e níveis de evidência (A, B, C).",
                tags = listOf("esc", "cardiologia", "diretrizes", "europa", "guidelines"),
                citation = "European Society of Cardiology", sourceUrl = "https://www.escardio.org", sourceRef = "Portal ESC"
            ),
            LibraryContentItem(
                id = "ref_084", title = "AHA/ACC — American Heart Association / American College of Cardiology",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes cardiovasculares americanas.",
                content = "A American Heart Association (AHA) e o American College of Cardiology (ACC) publicam conjuntamente diretrizes cardiovasculares de alto impacto. Incluem: manejo do IAM (STEMI e NSTEMI), insuficiência cardíaca, fibrilação atrial, doença valvar, cardiologia preventiva, e reabilitação cardíaca. Também publicam os guidelines de ACLS/PALS e estatísticas anuais de doenças cardiovasculares (Heart Disease and Stroke Statistics).",
                tags = listOf("aha", "acc", "cardiologia", "diretrizes", "eua"),
                citation = "American Heart Association", sourceUrl = "https://professional.heart.org", sourceRef = "Portal AHA/ACC"
            ),
            LibraryContentItem(
                id = "ref_085", title = "ESMO — European Society for Medical Oncology",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes em oncologia baseadas em evidências.",
                content = "A European Society for Medical Oncology (ESMO) publica diretrizes de prática clínica para o manejo de tumores sólidos e hematológicos. As diretrizes ESMO Grading (MCBS — Magnitude of Clinical Benefit Scale) classificam o benefício clínico das terapias oncológicas. Incluem: diagnóstico, estadiamento, tratamento (cirurgia, radioterapia, quimioterapia, terapia-alvo, imunoterapia) e acompanhamento. Publicadas no Annals of Oncology e ESMO Open.",
                tags = listOf("esmo", "oncologia", "diretrizes", "câncer", "guidelines"),
                citation = "European Society for Medical Oncology", sourceUrl = "https://www.esmo.org", sourceRef = "Portal ESMO"
            ),
            LibraryContentItem(
                id = "ref_086", title = "IDF — International Diabetes Federation",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes globais em diabetes.",
                content = "A International Diabetes Federation (IDF) publica diretrizes globais para o manejo do diabetes mellitus. Incluem: Diabetes Atlas (estimativas globais de prevalência), diretrizes de cuidados com o pé diabético, recomendações de rastreamento e diagnóstico, educação em diabetes, e prevenção. Colabora com a OMS e organizações nacionais (SBD no Brasil) para padronizar o cuidado ao paciente com diabetes globalmente.",
                tags = listOf("idf", "diabetes", "diretrizes", "global", "atlas"),
                citation = "International Diabetes Federation", sourceUrl = "https://www.idf.org", sourceRef = "Portal IDF"
            ),
            LibraryContentItem(
                id = "ref_087", title = "USPSTF — US Preventive Services Task Force",
                category = "CLINICA_MEDICA",
                summary = "Recomendações de rastreamento e prevenção baseadas em evidências.",
                content = "A US Preventive Services Task Force (USPSTF) é um painel independente de especialistas em prevenção e medicina baseada em evidências. Publica recomendações de rastreamento (screening) para diversas condições: câncer (mama, colo, cólon, pulmão, próstata), doenças cardiovasculares, diabetes, hepatites virais, HIV, osteoporose, e depressão. Cada recomendação recebe uma nota (A, B, C, D, I) baseada na qualidade da evidência e balanço riscos-benefícios.",
                tags = listOf("uspstf", "prevenção", "rastreamento", "screening", "evidência"),
                citation = "US Preventive Services Task Force", sourceUrl = "https://www.uspreventiveservicestaskforce.org", sourceRef = "Portal USPSTF"
            ),
            LibraryContentItem(
                id = "ref_088", title = "KDIGO — Kidney Disease: Improving Global Outcomes",
                category = "CLINICA_MEDICA",
                summary = "Diretrizes globais em nefrologia.",
                content = "O KDIGO (Kidney Disease: Improving Global Outcomes) publica diretrizes clínicas baseadas em evidências para o manejo de doenças renais. Incluem: doença renal crônica (classificação, estadiamento, tratamento), glomerulonefrites, hipertensão e DRC, diabetes e DRC, distúrbios mineral-ósseos, anemia, e transplante renal. As diretrizes KDIGO são adotadas como padrão internacional por sociedades de nefrologia em todo o mundo.",
                tags = listOf("kdigo", "nefrologia", "diretrizes", "drc", "global"),
                citation = "KDIGO", sourceUrl = "https://kdigo.org", sourceRef = "Portal KDIGO"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 12: PERIÓDICOS CIENTÍFICOS DE ALTO IMPACTO (89-94)
    // ═══════════════════════════════════════════════════════════════

    private val topJournalsPack = LibraryContentPack(
        source = "The Lancet, NEJM, Nature Medicine, JAMA, Annals of Internal Medicine, BMJ — periódicos de maior impacto",
        items = listOf(
            LibraryContentItem(
                id = "ref_089", title = "The Lancet — Periódico médico de referência global",
                category = "CLINICA_MEDICA",
                summary = "Um dos periódicos mais antigos e prestigiados do mundo desde 1823.",
                content = "The Lancet é um dos periódicos médicos mais antigos e prestigiados do mundo (fundado em 1823 por Thomas Wakley). Publica: artigos de pesquisa originais, revisões, comentários, séries temáticas (Lancet Series), e relatórios da Comissão Lancet (Lancet Commissions). Fator de impacto elevado. Séries especializadas: Lancet Oncology, Lancet Neurology, Lancet Infectious Diseases, Lancet Respiratory Medicine, Lancet Global Health, e Lancet Digital Health.",
                tags = listOf("the lancet", "periódico", "alto impacto", "pesquisa", "revisão"),
                citation = "Elsevier / The Lancet", sourceUrl = "https://www.thelancet.com", sourceRef = "Portal The Lancet"
            ),
            LibraryContentItem(
                id = "ref_090", title = "NEJM — New England Journal of Medicine",
                category = "CLINICA_MEDICA",
                summary = "Periódico médico com a mais rigorosa revisão por pares.",
                content = "O New England Journal of Medicine (NEJM) é o periódico médico mais citado do mundo, publicado pela Massachusetts Medical Society. Recebe cerca de 5.000 artigos/ano e publica apenas ~5% após revisão por pares rigorosa. Foco em: ensaios clínicos randomizados de alto impacto, revisões abrangentes (Review Articles), casos clínicos (Case Records of MGH), e perspectivas em políticas de saúde. Conteúdo de acesso aberto seletivo (artigos financiados por agências governamentais americanas).",
                tags = listOf("nejm", "periódico", "alto impacto", "ensaio clínico", "revisão"),
                citation = "Massachusetts Medical Society", sourceUrl = "https://www.nejm.org", sourceRef = "Portal NEJM"
            ),
            LibraryContentItem(
                id = "ref_091", title = "Nature Medicine — Pesquisa translacional",
                category = "CLINICA_MEDICA",
                summary = "Pesquisa translacional de ponta em biomedicina.",
                content = "Nature Medicine é um periódico mensal do grupo Nature que publica pesquisa translacional e clínica de ponta. Foco em: descobertas que conectam a pesquisa básica à aplicação clínica (bench-to-bedside). Áreas: oncologia, neurociências, cardiologia, genética, imunoterapia, edição genética (CRISPR), inteligência artificial em medicina, e medicina regenerativa. Fator de impacto entre os mais altos da biomedicina.",
                tags = listOf("nature medicine", "pesquisa translacional", "biomedicina", "alto impacto"),
                citation = "Springer Nature", sourceUrl = "https://www.nature.com/nm", sourceRef = "Nature Medicine"
            ),
            LibraryContentItem(
                id = "ref_092", title = "JAMA — Journal of the American Medical Association",
                category = "CLINICA_MEDICA",
                summary = "Revista médica com a maior tiragem mundial.",
                content = "O Journal of the American Medical Association (JAMA) é a revista médica com a maior circulação mundial (mais de 300.000 exemplares). Publica: artigos de pesquisa originais, revisões (JAMA Clinical Evidence Synopsis), guidelines clínicos, editoriais, e a série 'JAMA Clinical Guidelines'. Séries especializadas: JAMA Internal Medicine, JAMA Surgery, JAMA Pediatrics, JAMA Neurology, JAMA Psychiatry, JAMA Ophthalmology, JAMA Dermatology, e JAMA Oncology.",
                tags = listOf("jama", "periódico", "alto impacto", "ama", "pesquisa clínica"),
                citation = "American Medical Association", sourceUrl = "https://jamanetwork.com", sourceRef = "JAMA Network"
            ),
            LibraryContentItem(
                id = "ref_093", title = "Annals of Internal Medicine — American College of Physicians",
                category = "CLINICA_MEDICA",
                summary = "Periódico oficial do American College of Physicians.",
                content = "Os Annals of Internal Medicine são o periódico oficial do American College of Physicians (ACP). Foco em medicina interna: cardiologia, gastroenterologia, nefrologia, pneumologia, endocrinologia, reumatologia, infectologia, e geriatria. Publica: artigos originais, revisões sistemáticas, guidelines do ACP, e éticas clínicas. Conhecido pela série 'In the Clinic' — guias práticos para o ambulatório.",
                tags = listOf("annals internal medicine", "acp", "medicina interna", "periódico"),
                citation = "American College of Physicians", sourceUrl = "https://www.acpjournals.org", sourceRef = "Annals of Internal Medicine"
            ),
            LibraryContentItem(
                id = "ref_094", title = "BMJ — British Medical Journal",
                category = "CLINICA_MEDICA",
                summary = "Periódico de acesso aberto com forte apelo à prática clínica.",
                content = "O British Medical Journal (BMJ) é um periódico de acesso aberto que combina pesquisa original de alto impacto com conteúdo voltado para a prática clínica diária. Oferece: artigos de pesquisa, revisões clínicas (Clinical Reviews), análises (Analysis), editoriais, e a série 'BMJ Practice' (diretrizes, orientações). Pioneiro em acesso aberto e transparência de dados de pesquisa. Acesso gratuito a todo o conteúdo no site.",
                tags = listOf("bmj", "periódico", "acesso aberto", "prática clínica", "reino unido"),
                citation = "British Medical Journal", sourceUrl = "https://www.bmj.com", sourceRef = "Portal BMJ"
            ),
        )
    )

    // ═══════════════════════════════════════════════════════════════
    // PACOTE 13: RECURSOS EDUCACIONAIS E DE APOIO (95-100)
    // ═══════════════════════════════════════════════════════════════

    private val educationalResourcesPack = LibraryContentPack(
        source = "Medscape, MSD Manuals, UpToDate, Cleveland Clinic, Mayo Clinic, GP Evidence — recursos educacionais",
        items = listOf(
            LibraryContentItem(
                id = "ref_095", title = "Medscape — Portal de educação médica continuada",
                category = "CLINICA_MEDICA",
                summary = "Notícias médicas, artigos de revisão, educação continuada e referência clínica.",
                content = "Medscape é o maior portal de educação médica continuada (CME) do mundo, de acesso gratuito (financiado por publicidade). Oferece: notícias médicas atualizadas diariamente, artigos de revisão por especialidade, monografias de medicamentos e interações, calculadoras médicas, diretrizes clínicas, e milhares de atividades de CME/CPD com créditos. Cadastro gratuito necessário. Aplicativo móvel disponível com funcionalidades offline.",
                tags = listOf("medscape", "cme", "educação médica", "notícias", "gratuito"),
                citation = "Medscape (WebMD)", sourceUrl = "https://www.medscape.com", sourceRef = "Medscape"
            ),
            LibraryContentItem(
                id = "ref_096", title = "MSD Manuals — Manuais médicos gratuitos",
                category = "CLINICA_MEDICA",
                summary = "Manuais médicos (profissional e pacientes) com informações concisas sobre doenças.",
                content = "Os MSD Manuals (anteriormente Merck Manuals) são um recurso médico gratuito, sem fins lucrativos, da Merck Sharp & Dohme. Versão Profissional: informações abrangentes sobre etiologia, fisiopatologia, diagnóstico e tratamento de mais de 2.000 doenças. Versão para Pacientes: linguagem acessível. Conteúdo revisado por mais de 400 especialistas. Totalmente gratuito — sem cadastro, sem publicidade. Disponível online e como aplicativo.",
                tags = listOf("msd manuals", "merck manual", "livro", "referência", "gratuito"),
                citation = "Merck Sharp & Dohme", sourceUrl = "https://www.msdmanuals.com", sourceRef = "MSD Manuals"
            ),
            LibraryContentItem(
                id = "ref_097", title = "UpToDate — Base de referência clínica",
                category = "CLINICA_MEDICA",
                summary = "Base de dados de referência clínica com conteúdo revisado e atualizado continuamente.",
                content = "UpToDate é a principal referência clínica de ponto de cuidado (point-of-care) do mundo, usada por mais de 1,9 milhão de médicos em 190 países. Conteúdo desenvolvido por mais de 7.200 médicos especialistas (autores e revisores). Mais de 12.000 tópicos clínicos com recomendações graduadas (Grade 1-2). Atualizações contínuas baseadas em vigilância sistemática da literatura. Integração com EMR e Lexicomp. Acesso por assinatura individual ou institucional.",
                tags = listOf("uptodate", "referência clínica", "point-of-care", "decisão clínica"),
                citation = "Wolters Kluwer", sourceUrl = "https://www.uptodate.com", sourceRef = "UpToDate"
            ),
            LibraryContentItem(
                id = "ref_098", title = "Cleveland Clinic — Artigos de saúde baseados em evidências",
                category = "CLINICA_MEDICA",
                summary = "Artigos de saúde revisados por profissionais da Cleveland Clinic.",
                content = "O portal de saúde da Cleveland Clinic oferece milhares de artigos revisados por especialistas, cobrindo: doenças e condições (diagnóstico, tratamento, prevenção), procedimentos e exames, medicamentos, nutrição, bem-estar e estilo de vida. Conteúdo em linguagem acessível, baseado em evidências científicas. A Cleveland Clinic é consistentemente classificada entre os melhores hospitais dos EUA, especialmente em cardiologia e cirurgia cardíaca.",
                tags = listOf("cleveland clinic", "saúde", "artigos", "evidência", "paciente"),
                citation = "Cleveland Clinic", sourceUrl = "https://my.clevelandclinic.org/health", sourceRef = "Cleveland Clinic Health"
            ),
            LibraryContentItem(
                id = "ref_099", title = "Mayo Clinic — Informação de saúde confiável",
                category = "CLINICA_MEDICA",
                summary = "Informações de saúde revisadas por especialistas da Mayo Clinic.",
                content = "O portal de saúde e informação de paciente da Mayo Clinic oferece conteúdo revisado por seus especialistas. Cobre: sintomas e causas de doenças, diagnóstico e tratamento, guias de autocuidado, medicamentos e suplementos, testes e procedimentos, e estilo de vida saudável. Conteúdo baseado em evidências, revisado e atualizado regularmente. Recurso confiável para profissionais e pacientes, sem publicidade.",
                tags = listOf("mayo clinic", "saúde", "paciente", "evidência", "confiável"),
                citation = "Mayo Clinic", sourceUrl = "https://www.mayoclinic.org/patient-care-and-health-information", sourceRef = "Mayo Clinic Health Information"
            ),
            LibraryContentItem(
                id = "ref_100", title = "GP Evidence (University of Oxford) — Evidências para APS",
                category = "CLINICA_MEDICA",
                summary = "Resumos de evidências para medicina de família e atenção primária.",
                content = "GP Evidence é um recurso da University of Oxford (Department of Primary Care Health Sciences) que oferece resumos de evidências atualizados para medicina de família e atenção primária. Inclui: POEMs (Patient-Oriented Evidence that Matters), resumos estruturados de ensaios clínicos e revisões sistemáticas de relevância para a atenção primária. Ferramenta para prática clínica baseada em evidências no dia a dia do médico de família e comunidade.",
                tags = listOf("gp evidence", "oxford", "atenção primária", "evidência", "medicina família"),
                citation = "University of Oxford", sourceUrl = "https://www.gpevidence.org", sourceRef = "GP Evidence"
            ),
        )
    )

    /**
     * Todos os 13 pacotes disponíveis para importação via Curadoria.
     * Total: 100 referências médicas verificáveis.
     */
    val allPacks: List<LibraryContentPack> by lazy {
        listOf(
            whoPack,
            digitalLibrariesPack,
            freeBooksPack,
            universityPack,
            openAccessJournalsPack,
            additionalResourcesPack,
            msBrasilPack,
            anvisaPack,
            pharmacyPack,
            brazilianSocietiesPack,
            guidelinesPack,
            topJournalsPack,
            educationalResourcesPack,
        )
    }
}
