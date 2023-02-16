import {describe, expect, test} from '@jest/globals';
import { jqlApi } from '@/api/jqlApi'

function checkSorted(items, key, ascending) {
  let prev = items[0];
  for (let i = 1; i < items.length; i++) {
    const item = items[i];
    if (ascending) {
      expect(item[key].localeCompare(prev[key]) >= 0).toBeTruthy();
    } else {
      expect(item[key].localeCompare(prev[key]) <= 0).toBeTruthy();
    }
    prev = item;
  }
}

describe('Listing', () => {
  test('Find All', async () => {
    const count = await jqlApi.count();
    const res = await jqlApi.find();
    const characters = res.content;
    expect(characters.length).toBe(count);
  });

  test('Sort by name ascending order', async () => {
    const res = await jqlApi.find(null, { sort: "name" });
    const characters = res.content;
    expect(characters.length).toBeGreaterThan(0);
    checkSorted(characters, "name", true);
  });

  test('Sort by name descending order', async () => {
    const res = await jqlApi.find(null, { sort: "-name" });
    const characters = res.content;
    expect(characters.length).toBeGreaterThan(0);
    checkSorted(characters, "name", false);
  });

  test('Limit & Sort', async () => {
    const limit = 5
    const res = await jqlApi.find(null, { sort: "-name", limit });
    const characters = res.content;
    expect(characters.length).toBe(limit);
    checkSorted(characters, "name", false);
  });

  test('Pagination & Sort', async () => {
    const limit = 3;
    const page = 1;
    const res = await jqlApi.find(null, { sort: "-name", limit, page });
    const characters = res.content;
    expect(characters.length).toBeLessThanOrEqual(limit);
    expect(res.metadata.totalElements).toBeGreaterThanOrEqual(limit);
    checkSorted(characters, "name", false);
  });

});

