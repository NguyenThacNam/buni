/**
 * Dựng cây danh mục từ danh sách phẳng mà API /categories trả về.
 *
 * API trả mỗi danh mục kèm parentId (null = danh mục gốc), đã lọc sẵn danh mục
 * đang ẩn và xếp theo độ ưu tiên. Thanh lọc ở trang Khóa học và menu "Khóa học"
 * trên thanh điều hướng đều cần cùng một cây, nên gom về một chỗ.
 *
 * Danh mục có parentId mà cha không nằm trong danh sách (cha đã bị xóa) thì
 * coi như danh mục gốc — thà hiện ra còn hơn mất hẳn.
 */
export function dungCayDanhMuc(categories) {
  const ds = Array.isArray(categories) ? categories : [];
  const theoId = new Map(ds.map((c) => [c.id, c]));

  const goc = [];
  const conCua = new Map(); // id cha -> [con], giữ đúng thứ tự API đã xếp

  for (const c of ds) {
    if (c.parentId != null && theoId.has(c.parentId)) {
      if (!conCua.has(c.parentId)) conCua.set(c.parentId, []);
      conCua.get(c.parentId).push(c);
    } else {
      goc.push(c);
    }
  }

  /** Mọi con cháu của một danh mục, theo thứ tự duyệt sâu, kèm độ sâu. */
  const conChau = (id, doSau = 1) =>
    (conCua.get(id) || []).flatMap((c) => [
      { ...c, doSau },
      ...conChau(c.id, doSau + 1),
    ]);

  /** Danh mục gốc chứa slug này (chính nó nếu nó đã là gốc). */
  const gocCua = (slug) => {
    let c = ds.find((x) => x.slug === slug);
    for (let buoc = 0; c && buoc < 10; buoc++) {
      if (c.parentId == null || !theoId.has(c.parentId)) return c;
      c = theoId.get(c.parentId);
    }
    return null;
  };

  return { goc, conCua, conChau, gocCua };
}
