import {describe, expect, test} from '@jest/globals';
import { jqlApi } from '@/api/jqlApi'

describe('Top', () => {
  test('Find first', async () => {
    const filter = {
      "name@like": "Luke%"
    }
    const character = await jqlApi.top(filter);

    expect(character.name).toBe("Luke Skywalker");

    expect(character.name).toMatch("Sky");
    expect(character.name).toMatch(/Luke Skywalker/);
    expect(character.name).toMatch(/Luke .*/);

    expect(character.name.indexOf("Luke") == 0).toBeTruthy();
    expect(character.name.startsWith("Luke")).toBeTruthy();
  });

  test('Select PrimaryKeys only', async () => {
    const character = await jqlApi.top(null, { select: "0" });
    for (const k in character) {
      expect(k).toBe('id');
    }
  });

  test('Select Name only', async () => {
    const character = await jqlApi.top(null, { select: "name" });
    for (const k in character) {
      expect(k).toBe('name');
    }
  });

  test('Select PrimaryKeys and Name', async () => {
    const character = await jqlApi.top(null, { select: "0, name" });
    expect(character.id).not.toBeUndefined();
    expect(character.name).not.toBeUndefined();
    for (const k in character) {
      expect(k == 'id' || k == 'name').toBeTruthy();
    }
  });

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
});

