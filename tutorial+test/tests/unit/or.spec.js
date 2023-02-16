import {beforeAll, describe, expect, test} from '@jest/globals';
import { jqlApi } from '@/api/jqlApi'

const normal_height = [ 1.2, 2.0 ]

const normal_mass = [ 40, 120 ]

const MIN = 0, MAX = 1;

describe('Or operation', () => {
  let last_count;

  test('too_small_or_too_tall', async () => {
    const too_small_or_too_tall = {
      "height@not between": normal_height
    }
    
    const res = await jqlApi.find(too_small_or_too_tall);
    const characters = res.content;
    for (const character of characters) {
      expect(character.height < normal_height[MIN] || character.height > normal_height[MAX]).toBeTruthy();
    }
  });  

  test('too_light_or_too_heavy', async () => {
    const too_light_or_too_heavy = {
      "mass@not between": normal_mass
    }
    
    const res = await jqlApi.find(too_light_or_too_heavy);
    const characters = res.content;
    for (const character of characters) {
      expect(character.mass < normal_mass[MIN] || character.mass > normal_mass[MAX]).toBeTruthy();
    }
  });  

  
  test('too_small_or_too_heavy', async () => {
    const too_small_or_too_heavy = {
      "@not": {
          "height@ge": normal_height[MIN],
          "mass@le": normal_mass[MAX]
      }
    }
        
    const res = await jqlApi.find(too_small_or_too_heavy);
    const characters = res.content;
    for (const character of characters) {
      expect(character.height < normal_height[MIN] || character.mass > normal_mass[MAX]).toBeTruthy();
    }
  });    
});

