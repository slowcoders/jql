import {describe, expect, test} from '@jest/globals';
import { jqlApi } from '@/api/jqlApi'

describe('Join Test', () => {
  test('Find any character having a starship that length > 10', async () => {
    const filter = {
      "starship_": { "length@gt": 10 }
    }
    const character = await jqlApi.top(filter);
    for (const ship of character.starship_) {
      expect(ship.length).toBeGreaterThan(10);
    }
  });

  test('Find any character having a starship that length < 10', async () => {
    const filter = {
      "starship_": { "length@lt": 10 }
    }
    const character = await jqlApi.top(filter);
    for (const ship of character.starship_) {
      expect(ship.length).toBeLessThan(10);
    }
  });

  test('Find friends of Han Solo', async () => {
    const filter = {
      "name" : "Han Solo",
      "friend_": {}
    }
    const res = await jqlApi.find(filter);
    const characters = res.content;
    expect(characters.length).toBe(1);
    expect(characters[0].friend_.length).toBeGreaterThanOrEqual(3);
  });

  test('Find friends of Han Solo with joined query', async () => {
    const filter = {
      "name" : "Han Solo",
      "friend_": { "starship_": { "length@ge": 10 } }
    }
    const res = await jqlApi.find(filter);
    const characters = res.content;
    expect(characters.length).toBe(1);
    expect(characters[0].friend_.length).toBe(1);
  });
});

