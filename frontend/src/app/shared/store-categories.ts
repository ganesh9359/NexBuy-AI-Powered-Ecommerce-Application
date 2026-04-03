export interface StoreCategory {
  name: string;
  image: string;
  items: string[];
}

export const STORE_CATEGORIES: StoreCategory[] = [
  {
    name: 'Mobiles',
    image: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80',
    items: ['Smartphones', 'Cases & covers']
  },
  {
    name: 'Electronics',
    image: 'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=400&q=80',
    items: ['Audio gear', 'Smart devices']
  },
  {
    name: 'Fashion',
    image: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=400&q=80',
    items: ['Clothing', 'Footwear']
  },
  {
    name: 'Accessories',
    image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=400&q=80',
    items: ['Watches', 'Everyday carry']
  },
  {
    name: 'Home',
    image: 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=400&q=80',
    items: ['Home decor', 'Essentials']
  },
  {
    name: 'Books',
    image: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=400&q=80',
    items: ['Fiction', 'Study & reference']
  },
  {
    name: 'Shoes',
    image: 'https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=400&q=80',
    items: ['Casual', 'Sports']
  },
  {
    name: 'Gaming',
    image: 'https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=400&q=80',
    items: ['Consoles', 'Accessories']
  }
];