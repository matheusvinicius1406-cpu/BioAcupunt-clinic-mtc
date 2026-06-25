
export interface KnowledgeDTO {
  id?: string;
  title: string;
  slug: string;
  categoryId?: string;
  content: string;
  summary?: string;
  tags?: string[];
  references?: string[];
  isActive?: boolean;
}

export interface KnowledgeCategoryDTO {
  id?: string;
  name: string;
  description?: string;
  icon?: string;
  order?: number;
  isActive?: boolean;
  parentId?: string;
}
