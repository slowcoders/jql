import {beforeAll, describe, expect, test} from '@jest/globals';
import { jqlApi } from '@/api/jqlApi'

describe('And operation', () => {
  let last_count;
  const filter = {}

  beforeAll(async () => {
    const res = await jqlApi.find();
    last_count = res.content.length;

    const res2 = await jqlApi.find(null);
    expect(res2.content.length).toBe(last_count)

    const res3 = await jqlApi.find(filter);
    expect(res3.content.length).toBe(last_count)
  })

  test.each([
    { attr: "species", value: "Human"},
    { attr: "height@gt", value: 1.2},
    { attr: "height@lt", value: 2.0},
    { attr: "mass@gt", value: 60 },
    { attr: "metadata.memo.shoeSize@lt", value: 300 },
    { attr: "metadata.homePlanet", value: "Tatooine" }
  ]) ('And 조건 테스트', async ({attr, value}) => {
    filter[attr] = value;
    const res = await jqlApi.find(filter);
    const characters = res.content;
    expect(characters.length).toBeLessThan(last_count);
    last_count = characters.length;
  });  
});

